package com.mobile.core.domain.location

/** A WGS84 coordinate. Used by maps, routing, places and ride stops alike. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
