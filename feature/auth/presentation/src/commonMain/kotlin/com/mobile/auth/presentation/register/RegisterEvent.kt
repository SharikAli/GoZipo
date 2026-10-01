package com.mobile.auth.presentation.register

sealed interface RegisterEvent {
    data class Success(val email: String): RegisterEvent
}