package com.mobile.driving.presentation.routes

import com.mobile.core.domain.location.GeoPoint
import com.mobile.driving.presentation.navigation.NavigationTarget
import kotlinx.serialization.Serializable

/** Logged-in home: the map where the rider goes online, offers on requests and drives. */
@Serializable
data object HomeRoute

/** Add or edit the rider's vehicle; required before going online. */
@Serializable
data object VehicleRoute

@Serializable
data object EarningsRoute

/** In-app turn-by-turn guidance to the ride's next stop. */
@Serializable
data class TurnByTurnRoute(
    val rideId: String,
    val latitude: Double,
    val longitude: Double,
    val destinationName: String,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
) {
    fun toTarget() = NavigationTarget(
        rideId = rideId,
        destination = GeoPoint(latitude, longitude),
        destinationName = destinationName,
        origin = if (originLatitude != null && originLongitude != null) GeoPoint(originLatitude, originLongitude) else null
    )
}
