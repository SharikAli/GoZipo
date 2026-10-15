package com.mobile.driving.presentation.vehicle

sealed interface VehicleEvent {
    data object Saved : VehicleEvent
}
