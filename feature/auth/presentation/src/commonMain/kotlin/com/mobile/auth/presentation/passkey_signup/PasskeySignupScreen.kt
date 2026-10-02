package com.mobile.auth.presentation.passkey_signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.textfields.GoZipoCodeTextField
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.presentation.util.ObserveAsEvents
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.back
import gozipo.feature.auth.presentation.generated.resources.change_email
import gozipo.feature.auth.presentation.generated.resources.code_resent
import gozipo.feature.auth.presentation.generated.resources.create_passkey
import gozipo.feature.auth.presentation.generated.resources.email
import gozipo.feature.auth.presentation.generated.resources.email_placeholder
import gozipo.feature.auth.presentation.generated.resources.passkey_code_subtitle
import gozipo.feature.auth.presentation.generated.resources.passkey_code_title
import gozipo.feature.auth.presentation.generated.resources.passkey_signup_subtitle
import gozipo.feature.auth.presentation.generated.resources.passkey_signup_title
import gozipo.feature.auth.presentation.generated.resources.resend_code
import gozipo.feature.auth.presentation.generated.resources.send_code
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PasskeySignupRoot(
    viewModel: PasskeySignupViewModel = koinViewModel(),
    onSignupSuccess: () -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            PasskeySignupEvent.Success -> onSignupSuccess()
            PasskeySignupEvent.CodeResent -> {
                snackbarHostState.showSnackbar(getString(Res.string.code_resent))
            }
        }
    }

    PasskeySignupScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                PasskeySignupAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun PasskeySignupScreen(
    state: PasskeySignupState,
    onAction: (PasskeySignupAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val isCodeStep = state.step == PasskeySignupStep.ENTER_CODE

    GoZipoSnackbarScaffold(snackbarHostState = snackbarHostState) {
        GoZipoAuthFormLayout(
            title = if (isCodeStep) {
                stringResource(Res.string.passkey_code_title)
            } else {
                stringResource(Res.string.passkey_signup_title)
            },
            subtitle = if (isCodeStep) {
                stringResource(Res.string.passkey_code_subtitle, state.codeSentTo)
            } else {
                stringResource(Res.string.passkey_signup_subtitle)
            },
            errorText = state.error?.asString(),
            onBackClick = {
                onAction(
                    if (isCodeStep) PasskeySignupAction.OnChangeEmailClick else PasskeySignupAction.OnBackClick
                )
            },
            backContentDescription = stringResource(Res.string.back),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isCodeStep) {
                CodeStep(state = state, onAction = onAction)
            } else {
                EmailStep(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun EmailStep(
    state: PasskeySignupState,
    onAction: (PasskeySignupAction) -> Unit,
) {
    GoZipoTextField(
        state = state.emailTextFieldState,
        title = stringResource(Res.string.email),
        placeholder = stringResource(Res.string.email_placeholder),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send
        ),
        onKeyboardAction = { onAction(PasskeySignupAction.OnSendCodeClick) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(GoZipoTheme.spacing.xl))
    GoZipoButton(
        text = stringResource(Res.string.send_code),
        onClick = { onAction(PasskeySignupAction.OnSendCodeClick) },
        enabled = state.canSendCode || state.isSendingCode,
        isLoading = state.isSendingCode,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CodeStep(
    state: PasskeySignupState,
    onAction: (PasskeySignupAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoCodeTextField(
        state = state.codeTextFieldState,
        isError = state.isCodeError,
        enabled = !state.isCreatingPasskey,
        onKeyboardAction = { onAction(PasskeySignupAction.OnCreatePasskeyClick) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(spacing.xl))
    GoZipoButton(
        text = stringResource(Res.string.create_passkey),
        onClick = { onAction(PasskeySignupAction.OnCreatePasskeyClick) },
        enabled = state.canCreatePasskey || state.isCreatingPasskey,
        isLoading = state.isCreatingPasskey,
        leadingIcon = {
            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(spacing.md))
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        GoZipoButton(
            text = stringResource(Res.string.change_email),
            onClick = { onAction(PasskeySignupAction.OnChangeEmailClick) },
            style = GoZipoButtonStyle.TEXT,
            enabled = !state.isCreatingPasskey
        )
        GoZipoButton(
            text = stringResource(Res.string.resend_code),
            onClick = { onAction(PasskeySignupAction.OnResendCodeClick) },
            style = GoZipoButtonStyle.TEXT,
            isLoading = state.isSendingCode,
            enabled = !state.isCreatingPasskey
        )
    }
}

@Preview
@Composable
private fun PasskeySignupEmailPreview() {
    GoZipoTheme(darkTheme = true) {
        PasskeySignupScreen(
            state = PasskeySignupState(),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview
@Composable
private fun PasskeySignupCodePreview() {
    GoZipoTheme(darkTheme = true) {
        PasskeySignupScreen(
            state = PasskeySignupState(
                step = PasskeySignupStep.ENTER_CODE,
                codeSentTo = "you@example.com",
                codeTextFieldState = TextFieldState("4821")
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
