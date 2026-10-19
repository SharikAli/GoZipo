package com.mobile.core.map.api

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.mobile.core.domain.location.GeoPoint

/**
 * A map SDK behind [GoZipoMap]. Today `MapboxMapProvider` (:core:map:mapbox); a Google Maps
 * module would implement this and the domain's `PlaceSearchService`/`RouteService`, and the
 * apps would bind it in Koin instead. Features never see the implementation.
 *
 * Implementations must render [MapMarker.content] at the marker's position, draw
 * [polylines] in order, apply [MapCameraState.commands], and report camera movement through
 * [MapCameraState.onCameraChanged].
 */
interface MapProvider {
    @Composable
    fun Map(
        cameraState: MapCameraState,
        markers: List<MapMarker>,
        polylines: List<MapPolyline>,
        settings: MapSettings,
        onMapClick: ((GeoPoint) -> Unit)?,
        modifier: Modifier
    )
}

/**
 * @param showUserLocation draws the provider's own location indicator. Only pass true once
 * location permission is granted.
 * @param contentPadding area covered by app UI (e.g. the bottom sheet). The camera centers
 * inside the rest, and the provider keeps its logo and attribution visible.
 */
@Immutable
data class MapSettings(
    val darkTheme: Boolean,
    val showUserLocation: Boolean,
    val contentPadding: PaddingValues
)
