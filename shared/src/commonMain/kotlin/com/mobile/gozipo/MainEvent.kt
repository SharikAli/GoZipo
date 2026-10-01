package com.mobile.gozipo

sealed interface MainEvent {
    data object OnSessionExpired: MainEvent
}