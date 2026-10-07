package com.mobile.apps.common.home

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.route.Route

data class HomeMapState(
    val locationError: LocationError? = null,
    val userLocation: GeoPoint? = null,
    /** Center of the map while choosing pickup; fixed once a destination is set. */
    val pickup: GeoPoint? = null,
    val pickupAddress: String? = null,
    val isResolvingPickup: Boolean = false,
    val destination: Place? = null,
    val route: Route? = null,
    val isLoadingRoute: Boolean = false,
    val routeFailed: Boolean = false,
) {
    val showUserLocation: Boolean
        get() = userLocation != null
}
