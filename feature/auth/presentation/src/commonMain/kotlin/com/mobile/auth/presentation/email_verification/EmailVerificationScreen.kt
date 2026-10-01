package com.mobile.auth.presentation.email_verification

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    Text("Email Verification Screen")
}

@Preview
@Composable
private fun EmailVerificationErrorPreview() {
    EmailVerificationScreen(
        state = EmailVerificationState(),
        onAction = {}
    )
}

@Preview
@Composable
private fun EmailVerificationVerifyingPreview() {
    EmailVerificationScreen(
        state = EmailVerificationState(
            isVerifying = true
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun EmailVerificationSuccessPreview() {
    EmailVerificationScreen(
        state = EmailVerificationState(
            isVerified = true
        ),
        onAction = {}
    )
}