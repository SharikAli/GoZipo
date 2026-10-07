package com.mobile.apps.common.home

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.presentation.permissions.PermissionState

sealed interface HomeMapAction {
    data class OnLocationPermissionResult(val state: PermissionState) : HomeMapAction
    data class OnCameraIdle(val center: GeoPoint) : HomeMapAction
    data class OnMapClick(val point: GeoPoint) : HomeMapAction
    data object OnClearDestinationClick : HomeMapAction
    data object OnRecenterClick : HomeMapAction
    data object OnLogoutClick : HomeMapAction
}
