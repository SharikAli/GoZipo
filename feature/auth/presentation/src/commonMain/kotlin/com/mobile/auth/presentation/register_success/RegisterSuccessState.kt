package com.mobile.auth.presentation.register_success

import com.mobile.core.presentation.util.UiText

data class RegisterSuccessState(
    val registeredEmail: String = "",
    val isResendingVerificationEmail: Boolean = false,
    val resendVerificationError: UiText? = null,
    val isLoginVerification: Boolean = false
)