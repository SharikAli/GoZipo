package com.mobile.booking.presentation.booking

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.presentation.util.UiText

sealed interface BookingEvent {
    data object RequestLocationPermission : BookingEvent
    data class CenterOn(val point: GeoPoint) : BookingEvent
    data class FitPoints(val points: List<GeoPoint>) : BookingEvent
    data class ShowMessage(val message: UiText) : BookingEvent
}
