package com.mobile.core.map.mapbox.dto

import kotlinx.serialization.Serializable

/** Directions API v5 response with `geometries=geojson`. */
@Serializable
data class DirectionsResponseDto(
    val code: String,
    val routes: List<DirectionsRouteDto> = emptyList()
)

@Serializable
data class DirectionsRouteDto(
    /** Meters. */
    val distance: Double,
    /** Seconds. */
    val duration: Double,
    val geometry: LineStringDto
)

@Serializable
data class LineStringDto(
    /** `[longitude, latitude]` pairs. */
    val coordinates: List<List<Double>>
)
