package com.mobile.auth.presentation.login

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.auth.domain.authservice.AuthService
import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyError
import com.mobile.auth.domain.passkey.PasskeyService
import com.mobile.auth.presentation.passkey.toUiTextOrNull
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.domain.validation.EmailValidator
import com.mobile.core.presentation.util.UiText
import com.mobile.core.presentation.util.toUiText
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.error_email_not_verified
import gozipo.feature.auth.presentation.generated.resources.error_invalid_credentials
import gozipo.feature.auth.presentation.generated.resources.error_passkey_not_recognized
import gozipo.feature.auth.presentation.generated.resources.error_passkey_verification_failed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val passkeyService: PasskeyService,
    private val passkeyAuthenticator: PasskeyAuthenticator,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val eventChannel = Channel<LoginEvent>()
    val events = eventChannel.receiveAsFlow()

    private val isEmailValidFlow = snapshotFlow { state.value.emailTextFieldState.text.toString() }
        .map { email -> EmailValidator.validate(email) }
        .distinctUntilChanged()

    private val isPasswordNotBlankFlow =
        snapshotFlow { state.value.passwordTextFieldState.text.toString() }
            .map { it.isNotBlank() }
            .distinctUntilChanged()

    private val _state = MutableStateFlow(
        LoginState(isPasskeySupported = passkeyAuthenticator.isSupported)
    )
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeTextStates()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value
        )

    private val isBusyFlow = state
        .map { it.isLoggingIn || it.isPasskeyLoggingIn }
        .distinctUntilChanged()

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.OnLoginClick -> login()
            LoginAction.OnTogglePasswordVisibility -> {
                _state.update {
                    it.copy(
                        isPasswordVisible = !it.isPasswordVisible
                    )
                }
            }

            LoginAction.OnPasskeyLoginClick -> passkeyLogin()

            else -> Unit
        }
    }

    private fun observeTextStates() {
        combine(
            isEmailValidFlow,
            isPasswordNotBlankFlow,
            isBusyFlow
        ) { isEmailValid, isPasswordNotBlank, isBusy ->
            _state.update {
                it.copy(
                    canLogin = !isBusy && isEmailValid && isPasswordNotBlank
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun login() {
        if (!state.value.canLogin) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoggingIn = true,
                    error = null
                )
            }

            val email = state.value.emailTextFieldState.text.toString()
            val password = state.value.passwordTextFieldState.text.toString()

            authService
                .login(
                    email = email,
                    password = password
                )
                .onSuccess { authInfo ->
                    sessionStorage.set(authInfo)

                    _state.update {
                        it.copy(
                            isLoggingIn = false
                        )
                    }
                    eventChannel.send(LoginEvent.Success)
                }
                .onFailure { error ->
                    val errorMessage = when (error) {
                        DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_invalid_credentials)
                        DataError.Remote.FORBIDDEN -> {
                            eventChannel.send(LoginEvent.NavigateToEmailVerificationScreen(email))
                            UiText.Resource(Res.string.error_email_not_verified)
                        }

                        else -> error.toUiText()
                    }

                    _state.update {
                        it.copy(
                            error = errorMessage,
                            isLoggingIn = false
                        )
                    }
                }
        }
    }

    /**
     * Usernameless login: the system picker lists every passkey saved for
     * GoZipo, so the email field isn't needed.
     */
    private fun passkeyLogin() {
        if (state.value.isPasskeyLoggingIn || state.value.isLoggingIn) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isPasskeyLoggingIn = true,
                    error = null
                )
            }

            val ceremony = when (val result = passkeyService.loginOptions()) {
                is Result.Success -> result.data
                is Result.Failure -> {
                    onPasskeyLoginFailed(result.error.toPasskeyUiText())
                    return@launch
                }
            }

            val credentialJson =
                when (val result = passkeyAuthenticator.getPasskey(ceremony.requestJson)) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        onPasskeyLoginFailed(result.error.toUiTextOrNull())
                        if (result.error == PasskeyError.NO_CREDENTIAL) {
                            val email = state.value.emailTextFieldState.text.toString().trim()
                            eventChannel.send(
                                LoginEvent.NavigateToPasskeySignup(
                                    email = if (EmailValidator.validate(email)) email else ""
                                )
                            )
                        }
                        return@launch
                    }
                }

            passkeyService
                .loginVerify(
                    token = ceremony.token,
                    credentialJson = credentialJson
                )
                .onSuccess { authInfo ->
                    sessionStorage.set(authInfo)
                    _state.update {
                        it.copy(
                            isPasskeyLoggingIn = false
                        )
                    }
                    eventChannel.send(LoginEvent.Success)
                }
                .onFailure { error ->
                    onPasskeyLoginFailed(error.toPasskeyUiText())
                }
        }
    }

    private fun onPasskeyLoginFailed(error: UiText?) {
        _state.update {
            it.copy(
                isPasskeyLoggingIn = false,
                error = error
            )
        }
    }

    private fun DataError.Remote.toPasskeyUiText(): UiText = when (this) {
        DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_passkey_verification_failed)
        DataError.Remote.NOT_FOUND -> UiText.Resource(Res.string.error_passkey_not_recognized)
        else -> toUiText()
    }
}
