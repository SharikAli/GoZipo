package com.mobile.auth.presentation.reset_password

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.brand.GoZipoSuccessIcon
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoResultLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.textfields.GoZipoPasswordTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.back
import gozipo.feature.auth.presentation.generated.resources.hide_password
import gozipo.feature.auth.presentation.generated.resources.login
import gozipo.feature.auth.presentation.generated.resources.new_password
import gozipo.feature.auth.presentation.generated.resources.password_hint
import gozipo.feature.auth.presentation.generated.resources.reset_password_successfully
import gozipo.feature.auth.presentation.generated.resources.save_password
import gozipo.feature.auth.presentation.generated.resources.set_new_password
import gozipo.feature.auth.presentation.generated.resources.set_new_password_subtitle
import gozipo.feature.auth.presentation.generated.resources.show_password
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResetPasswordRoot(
    viewModel: ResetPasswordViewModel = koinViewModel(),
    onLoginClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ResetPasswordScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ResetPasswordAction.OnLoginClick -> onLoginClick()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onAction: (ResetPasswordAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoSnackbarScaffold {
        if (state.isResetSuccessful) {
            GoZipoResultLayout(
                title = stringResource(Res.string.set_new_password),
                description = stringResource(Res.string.reset_password_successfully),
                icon = { GoZipoSuccessIcon() },
                primaryButton = {
                    GoZipoButton(
                        text = stringResource(Res.string.login),
                        onClick = { onAction(ResetPasswordAction.OnLoginClick) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            return@GoZipoSnackbarScaffold
        }

        GoZipoAuthFormLayout(
            title = stringResource(Res.string.set_new_password),
            subtitle = stringResource(Res.string.set_new_password_subtitle),
            errorText = state.errorText?.asString(),
            onBackClick = { onAction(ResetPasswordAction.OnLoginClick) },
            backContentDescription = stringResource(Res.string.back),
            modifier = Modifier.fillMaxSize()
        ) {
            GoZipoPasswordTextField(
                state = state.passwordTextState,
                title = stringResource(Res.string.new_password),
                supportingText = stringResource(Res.string.password_hint),
                isPasswordVisible = state.isPasswordVisible,
                onToggleVisibilityClick = { onAction(ResetPasswordAction.OnTogglePasswordVisibilityClick) },
                showPasswordContentDescription = stringResource(Res.string.show_password),
                hidePasswordContentDescription = stringResource(Res.string.hide_password),
                onKeyboardAction = { onAction(ResetPasswordAction.OnSubmitClick) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing.xl))
            GoZipoButton(
                text = stringResource(Res.string.save_password),
                onClick = { onAction(ResetPasswordAction.OnSubmitClick) },
                enabled = state.canSubmit,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(apiLevel = 36)
@Composable
private fun Preview() {
    GoZipoTheme(darkTheme = true) {
        ResetPasswordScreen(
            state = ResetPasswordState(),
            onAction = {}
        )
    }
}
