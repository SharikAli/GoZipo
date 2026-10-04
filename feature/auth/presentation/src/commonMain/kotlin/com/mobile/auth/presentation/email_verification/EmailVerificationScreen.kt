package com.mobile.auth.presentation.email_verification

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.brand.GoZipoFailureIcon
import com.mobile.core.designsystem.components.brand.GoZipoSuccessIcon
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.layouts.GoZipoResultLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.close
import gozipo.feature.auth.presentation.generated.resources.email_verified_failed
import gozipo.feature.auth.presentation.generated.resources.email_verified_failed_desc
import gozipo.feature.auth.presentation.generated.resources.email_verified_successfully
import gozipo.feature.auth.presentation.generated.resources.email_verified_successfully_desc
import gozipo.feature.auth.presentation.generated.resources.login
import gozipo.feature.auth.presentation.generated.resources.verifying_account
import gozipo.feature.auth.presentation.generated.resources.verifying_account_desc
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmailVerificationRoot(
    viewModel: EmailVerificationViewModel = koinViewModel(),
    onLoginClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    EmailVerificationScreen(
        state = state,
        onAction = { action ->
            when (action) {
                EmailVerificationAction.OnCloseClick -> onCloseClick()
                EmailVerificationAction.OnLoginClick -> onLoginClick()
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun EmailVerificationScreen(
    state: EmailVerificationState,
    onAction: (EmailVerificationAction) -> Unit,
) {
    GoZipoSnackbarScaffold {
        when {
            state.isVerifying -> {
                GoZipoResultLayout(
                    title = stringResource(Res.string.verifying_account),
                    description = stringResource(Res.string.verifying_account_desc),
                    icon = {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(72.dp)
                        )
                    }
                )
            }

            state.isVerified -> {
                GoZipoResultLayout(
                    title = stringResource(Res.string.email_verified_successfully),
                    description = stringResource(Res.string.email_verified_successfully_desc),
                    icon = { GoZipoSuccessIcon() },
                    primaryButton = {
                        GoZipoButton(
                            text = stringResource(Res.string.login),
                            onClick = { onAction(EmailVerificationAction.OnLoginClick) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }

            else -> {
                GoZipoResultLayout(
                    title = stringResource(Res.string.email_verified_failed),
                    description = stringResource(Res.string.email_verified_failed_desc),
                    icon = { GoZipoFailureIcon() },
                    primaryButton = {
                        GoZipoButton(
                            text = stringResource(Res.string.close),
                            onClick = { onAction(EmailVerificationAction.OnCloseClick) },
                            style = GoZipoButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun EmailVerificationErrorPreview() {
    GoZipoTheme(darkTheme = true) {
        EmailVerificationScreen(
            state = EmailVerificationState(isVerifying = false),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EmailVerificationVerifyingPreview() {
    GoZipoTheme(darkTheme = true) {
        EmailVerificationScreen(
            state = EmailVerificationState(
                isVerifying = true
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun EmailVerificationSuccessPreview() {
    GoZipoTheme(darkTheme = false) {
        EmailVerificationScreen(
            state = EmailVerificationState(
                isVerifying = false,
                isVerified = true
            ),
            onAction = {}
        )
    }
}
