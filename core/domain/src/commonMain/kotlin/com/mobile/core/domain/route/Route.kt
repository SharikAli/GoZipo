package com.mobile.core.domain.route

import com.mobile.core.domain.location.GeoPoint
import kotlin.time.Duration

/**
 * A driving route. [points] is the full line to draw on the map. The backend computes its
 * own route for fares, so these numbers are for display only.
 */
data class Route(
    val distanceMeters: Int,
    val duration: Duration,
    val points: List<GeoPoint>
)
