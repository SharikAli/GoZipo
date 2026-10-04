package com.mobile.auth.presentation.email_verification

data class EmailVerificationState(
    // Verification starts as soon as the screen opens; avoids flashing the failure state
    val isVerifying: Boolean = true,
    val isVerified: Boolean = false
)
