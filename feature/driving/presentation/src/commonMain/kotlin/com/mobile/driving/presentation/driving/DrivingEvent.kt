package com.mobile.driving.presentation.driving

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.presentation.util.UiText

sealed interface DrivingEvent {
    data object RequestLocationPermission : DrivingEvent
    data object OpenVehicleSetup : DrivingEvent
    data class CenterOn(val point: GeoPoint) : DrivingEvent
    data class FitPoints(val points: List<GeoPoint>) : DrivingEvent
    data class ShowMessage(val message: UiText) : DrivingEvent
}
