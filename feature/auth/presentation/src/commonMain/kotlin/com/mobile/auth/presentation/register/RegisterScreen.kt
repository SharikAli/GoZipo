package com.mobile.auth.presentation.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.auth.presentation.components.AuthFooterLink
import com.mobile.auth.presentation.components.OrDivider
import com.mobile.auth.presentation.components.PasskeyButton
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.textfields.GoZipoPasswordTextField
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.presentation.util.ObserveAsEvents
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.create_account
import gozipo.feature.auth.presentation.generated.resources.create_account_subtitle
import gozipo.feature.auth.presentation.generated.resources.create_account_title
import gozipo.feature.auth.presentation.generated.resources.email
import gozipo.feature.auth.presentation.generated.resources.email_placeholder
import gozipo.feature.auth.presentation.generated.resources.have_account
import gozipo.feature.auth.presentation.generated.resources.hide_password
import gozipo.feature.auth.presentation.generated.resources.login
import gozipo.feature.auth.presentation.generated.resources.password
import gozipo.feature.auth.presentation.generated.resources.password_hint
import gozipo.feature.auth.presentation.generated.resources.password_placeholder
import gozipo.feature.auth.presentation.generated.resources.show_password
import gozipo.feature.auth.presentation.generated.resources.username
import gozipo.feature.auth.presentation.generated.resources.username_hint
import gozipo.feature.auth.presentation.generated.resources.username_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    viewModel: RegisterViewModel = koinViewModel(),
    onRegisterSuccess: (String) -> Unit,
    onLoginClick: () -> Unit,
    onPasskeySignupClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterEvent.Success -> {
                onRegisterSuccess(event.email)
            }
        }
    }

    RegisterScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is RegisterAction.OnLoginClick -> onLoginClick()
                is RegisterAction.OnPasskeySignupClick -> onPasskeySignupClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoSnackbarScaffold(snackbarHostState = snackbarHostState) {
        GoZipoAuthFormLayout(
            title = stringResource(Res.string.create_account_title),
            subtitle = stringResource(Res.string.create_account_subtitle),
            errorText = state.registrationError?.asString(),
            modifier = Modifier.fillMaxSize()
        ) {
            GoZipoTextField(
                state = state.usernameTextState,
                title = stringResource(Res.string.username),
                placeholder = stringResource(Res.string.username_placeholder),
                supportingText = state.usernameError?.asString()
                    ?: stringResource(Res.string.username_hint),
                isError = state.usernameError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.lg))
            GoZipoTextField(
                state = state.emailTextState,
                title = stringResource(Res.string.email),
                placeholder = stringResource(Res.string.email_placeholder),
                supportingText = state.emailError?.asString(),
                isError = state.emailError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.lg))
            GoZipoPasswordTextField(
                state = state.passwordTextState,
                title = stringResource(Res.string.password),
                placeholder = stringResource(Res.string.password_placeholder),
                supportingText = state.passwordError?.asString()
                    ?: stringResource(Res.string.password_hint),
                isError = state.passwordError != null,
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = { onAction(RegisterAction.OnTogglePasswordVisibilityClick) },
                showPasswordContentDescription = stringResource(Res.string.show_password),
                hidePasswordContentDescription = stringResource(Res.string.hide_password),
                onKeyboardAction = { onAction(RegisterAction.OnRegisterClick) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.xl))
            GoZipoButton(
                text = stringResource(Res.string.create_account),
                onClick = { onAction(RegisterAction.OnRegisterClick) },
                enabled = state.canRegister || state.isRegistering,
                isLoading = state.isRegistering,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isPasskeySupported) {
                Spacer(modifier = Modifier.height(spacing.xl))
                OrDivider()
                Spacer(modifier = Modifier.height(spacing.xl))
                PasskeyButton(
                    onClick = { onAction(RegisterAction.OnPasskeySignupClick) },
                    enabled = !state.isRegistering
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))
            AuthFooterLink(
                prompt = stringResource(Res.string.have_account),
                action = stringResource(Res.string.login),
                onClick = { onAction(RegisterAction.OnLoginClick) }
            )
        }
    }
}

@Preview(apiLevel = 36)
@Composable
private fun RegisterScreenPreview() {
    GoZipoTheme(darkTheme = true) {
        RegisterScreen(
            state = RegisterState(isPasskeySupported = true),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
