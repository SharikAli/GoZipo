package com.mobile.auth.presentation.login

sealed interface LoginEvent {
    data object Success : LoginEvent
    data class NavigateToEmailVerificationScreen(val email: String) : LoginEvent

    /** The device has no passkey for GoZipo: sign up for one with an emailed code. */
    data class NavigateToPasskeySignup(val email: String) : LoginEvent
}
