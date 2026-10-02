package com.mobile.auth.presentation.register_success

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.brand.GoZipoEmailSentIcon
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.layouts.GoZipoResultLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.presentation.util.ObserveAsEvents
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.account_successfully_created
import gozipo.feature.auth.presentation.generated.resources.login
import gozipo.feature.auth.presentation.generated.resources.resend_verification_email
import gozipo.feature.auth.presentation.generated.resources.resent_verification_email
import gozipo.feature.auth.presentation.generated.resources.verification_email_sent_to_x
import gozipo.feature.auth.presentation.generated.resources.verify_your_email
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterSuccessRoot(
    viewModel: RegisterSuccessViewModel = koinViewModel(),
    onLoginClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterSuccessEvent.ResendVerificationEmailSuccess -> {
                snackbarHostState.showSnackbar(
                    message = getString(
                        resource = Res.string.resent_verification_email
                    )
                )
            }
        }
    }

    RegisterSuccessScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is RegisterSuccessAction.OnLoginClick -> onLoginClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun RegisterSuccessScreen(
    state: RegisterSuccessState,
    onAction: (RegisterSuccessAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    GoZipoSnackbarScaffold(snackbarHostState = snackbarHostState) {
        GoZipoResultLayout(
            title = if (state.isLoginVerification)
                stringResource(Res.string.verify_your_email)
            else
                stringResource(Res.string.account_successfully_created),
            description = stringResource(
                Res.string.verification_email_sent_to_x,
                state.registeredEmail
            ),
            icon = { GoZipoEmailSentIcon() },
            primaryButton = {
                GoZipoButton(
                    text = stringResource(Res.string.login),
                    onClick = { onAction(RegisterSuccessAction.OnLoginClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            secondaryButton = {
                GoZipoButton(
                    text = stringResource(Res.string.resend_verification_email),
                    onClick = { onAction(RegisterSuccessAction.OnResendVerificationEmailClick) },
                    style = GoZipoButtonStyle.SECONDARY,
                    isLoading = state.isResendingVerificationEmail,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            secondaryError = state.resendVerificationError?.asString()
        )
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    GoZipoTheme {
        RegisterSuccessScreen(
            state = RegisterSuccessState(
                registeredEmail = "test@preview.com"
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
