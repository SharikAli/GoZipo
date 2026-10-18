package com.mobile.core.map.api

import com.mobile.core.domain.location.GeoPoint

/**
 * @param zoom web-mercator zoom level (0 = world, ~15 = streets); the same scale for every provider
 * @param bearing map rotation in degrees clockwise from north
 */
data class MapCameraPosition(
    val target: GeoPoint,
    val zoom: Double = STREET_ZOOM,
    val bearing: Double = 0.0
) {
    companion object {
        const val CITY_ZOOM = 11.0
        const val STREET_ZOOM = 15.5

        /** Limits of the +/- buttons: about a country, and single buildings. */
        const val MIN_ZOOM = 3.0
        const val MAX_ZOOM = 20.0

        /** Shown until the user's location is known: Karachi. */
        val DEFAULT = MapCameraPosition(
            target = GeoPoint(latitude = 24.8607, longitude = 67.0011),
            zoom = CITY_ZOOM
        )
    }
}
