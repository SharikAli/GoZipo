package com.mobile.auth.presentation.passkey_signup

sealed interface PasskeySignupEvent {
    data object CodeResent: PasskeySignupEvent
    data object Success: PasskeySignupEvent
}
