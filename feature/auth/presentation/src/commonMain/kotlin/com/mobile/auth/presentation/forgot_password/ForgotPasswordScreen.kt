package com.mobile.auth.presentation.forgot_password

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.back
import gozipo.feature.auth.presentation.generated.resources.email
import gozipo.feature.auth.presentation.generated.resources.email_placeholder
import gozipo.feature.auth.presentation.generated.resources.forgot_password_email_sent_successfully
import gozipo.feature.auth.presentation.generated.resources.forgot_password_subtitle
import gozipo.feature.auth.presentation.generated.resources.forgot_password_title
import gozipo.feature.auth.presentation.generated.resources.send_reset_link
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForgotPasswordRoot(
    viewModel: ForgotPasswordViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ForgotPasswordAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onAction: (ForgotPasswordAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoSnackbarScaffold {
        GoZipoAuthFormLayout(
            title = stringResource(Res.string.forgot_password_title),
            subtitle = stringResource(Res.string.forgot_password_subtitle),
            errorText = state.errorText?.asString(),
            onBackClick = { onAction(ForgotPasswordAction.OnBackClick) },
            backContentDescription = stringResource(Res.string.back),
            modifier = Modifier.fillMaxSize()
        ) {
            GoZipoTextField(
                state = state.emailTextFieldState,
                title = stringResource(Res.string.email),
                placeholder = stringResource(Res.string.email_placeholder),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Send
                ),
                onKeyboardAction = { onAction(ForgotPasswordAction.OnSubmitClick) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.xl))
            GoZipoButton(
                text = stringResource(Res.string.send_reset_link),
                onClick = { onAction(ForgotPasswordAction.OnSubmitClick) },
                enabled = state.canSubmit,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isEmailSentSuccessfully) {
                Spacer(modifier = Modifier.height(spacing.lg))
                Text(
                    text = stringResource(Res.string.forgot_password_email_sent_successfully),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.extended.success
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GoZipoTheme(darkTheme = true) {
        ForgotPasswordScreen(
            state = ForgotPasswordState(isEmailSentSuccessfully = true),
            onAction = {}
        )
    }
}
