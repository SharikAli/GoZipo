package com.mobile.driving.presentation.navigation

import com.mobile.core.domain.location.GeoPoint

/** Where turn-by-turn guidance goes for a ride's current leg (the pickup, then the destination). */
data class NavigationTarget(
    val rideId: String,
    val destination: GeoPoint,
    val destinationName: String,
    /** The rider's last known position, so routing can start before the SDK's own fix. */
    val origin: GeoPoint?,
)
