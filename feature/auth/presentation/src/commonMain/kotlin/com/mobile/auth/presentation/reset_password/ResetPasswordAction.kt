package com.mobile.auth.presentation.reset_password

sealed interface ResetPasswordAction {
    data object OnSubmitClick: ResetPasswordAction
    data object OnTogglePasswordVisibilityClick: ResetPasswordAction
    data object OnLoginClick: ResetPasswordAction
}
