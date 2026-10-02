package com.mobile.auth.presentation.passkey_signup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyCeremony
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
import gozipo.feature.auth.presentation.generated.resources.error_account_exists
import gozipo.feature.auth.presentation.generated.resources.error_passkey_already_registered
import gozipo.feature.auth.presentation.generated.resources.error_passkey_expired
import gozipo.feature.auth.presentation.generated.resources.error_passkey_invalid_code
import gozipo.feature.auth.presentation.generated.resources.error_passkey_verification_failed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Passkey sign-up for a device with no GoZipo passkey: email -> 6-digit
 * code -> system "create passkey" sheet -> signed in. Creates the account,
 * or adds the passkey to an existing account with that email.
 */
class PasskeySignupViewModel(
    private val passkeyService: PasskeyService,
    private val passkeyAuthenticator: PasskeyAuthenticator,
    private val sessionStorage: SessionStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        const val CODE_LENGTH = 6
    }

    private var hasLoadedInitialData = false

    private val _event = MutableSharedFlow<PasskeySignupEvent>()
    val events = _event.asSharedFlow()

    /**
     * Options from a verified code. The code is single use, so if the user
     * dismisses the system sheet we retry with these until they expire.
     */
    private var pendingCeremony: PasskeyCeremony? = null

    private val _state = MutableStateFlow(
        PasskeySignupState(
            emailTextFieldState = TextFieldState(
                initialText = savedStateHandle.get<String>("email").orEmpty()
            )
        )
    )
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeValidation()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value
        )

    private val isEmailValidFlow = snapshotFlow { _state.value.emailTextFieldState.text.toString() }
        .map { EmailValidator.validate(it.trim()) }
        .distinctUntilChanged()

    private val isCodeCompleteFlow = snapshotFlow { _state.value.codeTextFieldState.text.toString() }
        .map { it.length == CODE_LENGTH }
        .distinctUntilChanged()

    fun onAction(action: PasskeySignupAction) {
        when (action) {
            PasskeySignupAction.OnSendCodeClick -> sendCode(isResend = false)
            PasskeySignupAction.OnResendCodeClick -> sendCode(isResend = true)
            PasskeySignupAction.OnChangeEmailClick -> changeEmail()
            PasskeySignupAction.OnCreatePasskeyClick -> createPasskey()
            PasskeySignupAction.OnBackClick -> Unit
        }
    }

    private fun observeValidation() {
        combine(
            isEmailValidFlow,
            isCodeCompleteFlow,
            _state.map { it.isSendingCode || it.isCreatingPasskey }.distinctUntilChanged()
        ) { isEmailValid, isCodeComplete, isBusy ->
            _state.update {
                it.copy(
                    canSendCode = isEmailValid && !isBusy,
                    canCreatePasskey = isCodeComplete && !isBusy
                )
            }
        }.launchIn(viewModelScope)

        // Editing the code clears its error
        snapshotFlow { _state.value.codeTextFieldState.text.toString() }
            .distinctUntilChanged()
            .onEach {
                _state.update { state ->
                    if (state.isCodeError) state.copy(isCodeError = false, error = null) else state
                }
            }
            .launchIn(viewModelScope)
    }

    private fun sendCode(isResend: Boolean) {
        val current = _state.value
        if (current.isSendingCode || current.isCreatingPasskey) {
            return
        }
        val email = current.emailTextFieldState.text.toString().trim()
        if (!EmailValidator.validate(email)) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSendingCode = true,
                    error = null,
                    isCodeError = false
                )
            }

            passkeyService
                .sendSignupCode(email)
                .onSuccess {
                    // A new code invalidates options from the old one
                    pendingCeremony = null
                    _state.value.codeTextFieldState.clearText()
                    _state.update {
                        it.copy(
                            isSendingCode = false,
                            step = PasskeySignupStep.ENTER_CODE,
                            codeSentTo = email
                        )
                    }
                    if (isResend) {
                        _event.emit(PasskeySignupEvent.CodeResent)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSendingCode = false,
                            error = error.toSignupUiText()
                        )
                    }
                }
        }
    }

    private fun changeEmail() {
        pendingCeremony = null
        _state.value.codeTextFieldState.clearText()
        _state.update {
            it.copy(
                step = PasskeySignupStep.ENTER_EMAIL,
                error = null,
                isCodeError = false
            )
        }
    }

    private fun createPasskey() {
        val current = _state.value
        if (current.isCreatingPasskey || current.isSendingCode) {
            return
        }
        val code = current.codeTextFieldState.text.toString()
        if (pendingCeremony == null && code.length != CODE_LENGTH) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isCreatingPasskey = true,
                    error = null,
                    isCodeError = false
                )
            }

            val ceremony = pendingCeremony ?: when (
                val result = passkeyService.signupOptions(email = current.codeSentTo, code = code)
            ) {
                is Result.Success -> result.data.also { pendingCeremony = it }
                is Result.Failure -> {
                    val isInvalidCode = result.error == DataError.Remote.BAD_REQUEST
                    _state.update {
                        it.copy(
                            isCreatingPasskey = false,
                            isCodeError = isInvalidCode,
                            error = if (isInvalidCode) {
                                UiText.Resource(Res.string.error_passkey_invalid_code)
                            } else {
                                result.error.toSignupUiText()
                            }
                        )
                    }
                    return@launch
                }
            }

            val credentialJson = when (val result = passkeyAuthenticator.createPasskey(ceremony.requestJson)) {
                is Result.Success -> result.data
                is Result.Failure -> {
                    // Keep pendingCeremony: "Create passkey" can reopen the sheet without a new code
                    _state.update {
                        it.copy(
                            isCreatingPasskey = false,
                            error = result.error.toUiTextOrNull()
                        )
                    }
                    return@launch
                }
            }

            // Verifying consumes the ceremony, whatever the outcome
            pendingCeremony = null
            passkeyService
                .signupVerify(
                    token = ceremony.token,
                    credentialJson = credentialJson
                )
                .onSuccess { authInfo ->
                    sessionStorage.set(authInfo)
                    _state.update { it.copy(isCreatingPasskey = false) }
                    _event.emit(PasskeySignupEvent.Success)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCreatingPasskey = false,
                            error = when (error) {
                                DataError.Remote.UNAUTHORIZED -> UiText.Resource(Res.string.error_passkey_verification_failed)
                                DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_passkey_already_registered)
                                else -> error.toSignupUiText()
                            }
                        )
                    }
                }
        }
    }

    private fun DataError.Remote.toSignupUiText(): UiText = when (this) {
        // The 5-minute ceremony expired: options must come from a new code
        DataError.Remote.GONE -> UiText.Resource(Res.string.error_passkey_expired)
        DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
        else -> toUiText()
    }
}
