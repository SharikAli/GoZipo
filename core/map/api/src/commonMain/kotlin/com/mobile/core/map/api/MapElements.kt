package com.mobile.core.map.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.domain.location.GeoPoint

/**
 * A marker drawn with Compose (e.g. the design system's `PickupMarker`, `RiderMarker`),
 * so marker visuals stay the same whatever the provider.
 *
 * @param id stable key; providers reuse the native view for the same id
 */
@Immutable
class MapMarker(
    val id: String,
    val position: GeoPoint,
    val anchor: MarkerAnchor = MarkerAnchor.BOTTOM,
    val content: @Composable () -> Unit
)

enum class MarkerAnchor {
    /** Pins: the tip touches the point. */
    BOTTOM,

    /** Vehicles and dots: centered on the point. */
    CENTER
}

/**
 * A line on the map, such as a route.
 *
 * @param casingColor optional outline drawn under the line
 */
@Immutable
data class MapPolyline(
    val id: String,
    val points: List<GeoPoint>,
    val color: Color,
    val width: Dp = 5.dp,
    val casingColor: Color? = null
)
