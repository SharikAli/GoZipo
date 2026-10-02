package com.mobile.auth.presentation.passkey_signup

sealed interface PasskeySignupAction {
    data object OnBackClick: PasskeySignupAction
    data object OnSendCodeClick: PasskeySignupAction
    data object OnResendCodeClick: PasskeySignupAction
    data object OnChangeEmailClick: PasskeySignupAction
    data object OnCreatePasskeyClick: PasskeySignupAction
}
