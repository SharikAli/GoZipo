package com.mobile.apps.common

sealed interface MainEvent {
    data object OnSessionExpired: MainEvent
}