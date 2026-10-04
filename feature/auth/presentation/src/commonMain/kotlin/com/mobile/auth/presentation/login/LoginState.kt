package com.mobile.auth.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import com.mobile.core.domain.auth.UserRole
import com.mobile.core.presentation.util.UiText

data class LoginState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isPasskeySupported: Boolean = false,
    val isPasskeyLoggingIn: Boolean = false,
    val error: UiText? = null,
    val role: UserRole = UserRole.CUSTOMER
)
