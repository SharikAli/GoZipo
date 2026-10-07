package com.mobile.apps.common.home

import com.mobile.core.domain.location.GeoPoint

sealed interface HomeMapEvent {
    data object RequestLocationPermission : HomeMapEvent
    data class CenterOn(val point: GeoPoint) : HomeMapEvent
    data class ShowRoute(val points: List<GeoPoint>) : HomeMapEvent
}
