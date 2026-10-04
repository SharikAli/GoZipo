package com.mobile.auth.presentation.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.auth.presentation.components.AuthFooterLink
import com.mobile.auth.presentation.components.OrDivider
import com.mobile.auth.presentation.components.PasskeyButton
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.textfields.GoZipoPasswordTextField
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.domain.auth.UserRole
import com.mobile.core.presentation.util.ObserveAsEvents
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.create_account
import gozipo.feature.auth.presentation.generated.resources.email
import gozipo.feature.auth.presentation.generated.resources.email_placeholder
import gozipo.feature.auth.presentation.generated.resources.forgot_password
import gozipo.feature.auth.presentation.generated.resources.hide_password
import gozipo.feature.auth.presentation.generated.resources.login
import gozipo.feature.auth.presentation.generated.resources.login_subtitle
import gozipo.feature.auth.presentation.generated.resources.login_subtitle_rider
import gozipo.feature.auth.presentation.generated.resources.no_account
import gozipo.feature.auth.presentation.generated.resources.password
import gozipo.feature.auth.presentation.generated.resources.password_placeholder
import gozipo.feature.auth.presentation.generated.resources.show_password
import gozipo.feature.auth.presentation.generated.resources.welcome_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    navigateToEmailVerificationScreen: (String) -> Unit,
    navigateToPasskeySignup: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.Success -> onLoginSuccess()
            is LoginEvent.NavigateToEmailVerificationScreen -> navigateToEmailVerificationScreen(
                event.email
            )

            is LoginEvent.NavigateToPasskeySignup -> navigateToPasskeySignup(event.email)
        }
    }

    LoginScreen(
        state = state,
        onAction = { action ->
            when (action) {
                LoginAction.OnForgotPasswordClick -> onForgotPasswordClick()
                LoginAction.OnSignUpClick -> onCreateAccountClick()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoSnackbarScaffold {
        GoZipoAuthFormLayout(
            title = stringResource(Res.string.welcome_back),
            subtitle = when (state.role) {
                UserRole.CUSTOMER -> stringResource(Res.string.login_subtitle)
                UserRole.RIDER -> stringResource(Res.string.login_subtitle_rider)
            },
            errorText = state.error?.asString(),
            modifier = Modifier.fillMaxSize()
        ) {
            GoZipoTextField(
                state = state.emailTextFieldState,
                title = stringResource(Res.string.email),
                placeholder = stringResource(Res.string.email_placeholder),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.extended.textTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.lg))
            GoZipoPasswordTextField(
                state = state.passwordTextFieldState,
                title = stringResource(Res.string.password),
                placeholder = stringResource(Res.string.password_placeholder),
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = { onAction(LoginAction.OnTogglePasswordVisibility) },
                showPasswordContentDescription = stringResource(Res.string.show_password),
                hidePasswordContentDescription = stringResource(Res.string.hide_password),
                onKeyboardAction = { onAction(LoginAction.OnLoginClick) },
                modifier = Modifier.fillMaxWidth()
            )
            GoZipoButton(
                text = stringResource(Res.string.forgot_password),
                onClick = { onAction(LoginAction.OnForgotPasswordClick) },
                style = GoZipoButtonStyle.TEXT,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            GoZipoButton(
                text = stringResource(Res.string.login),
                onClick = { onAction(LoginAction.OnLoginClick) },
                enabled = state.canLogin || state.isLoggingIn,
                isLoading = state.isLoggingIn,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isPasskeySupported) {
                Spacer(modifier = Modifier.height(spacing.xl))
                OrDivider()
                Spacer(modifier = Modifier.height(spacing.xl))
                PasskeyButton(
                    onClick = { onAction(LoginAction.OnPasskeyLoginClick) },
                    isLoading = state.isPasskeyLoggingIn,
                    enabled = !state.isLoggingIn
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))
            AuthFooterLink(
                prompt = stringResource(Res.string.no_account),
                action = stringResource(Res.string.create_account),
                onClick = { onAction(LoginAction.OnSignUpClick) }
            )
        }
    }
}

@Preview(apiLevel = 36)
@Composable
private fun LoginScreenDarkPreview() {
    GoZipoTheme(darkTheme = true) {
        LoginScreen(
            state = LoginState(isPasskeySupported = true),
            onAction = {}
        )
    }
}

@Preview(apiLevel = 36)
@Composable
private fun LoginScreenLightPreview() {
    GoZipoTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(isPasskeySupported = true, isPasskeyLoggingIn = true),
            onAction = {}
        )
    }
}
