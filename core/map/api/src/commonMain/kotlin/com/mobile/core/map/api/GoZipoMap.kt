package com.mobile.core.map.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.mobile.core.domain.location.GeoPoint
import org.koin.compose.koinInject

/**
 * The app's map. Features use only this; the SDK comes from the [MapProvider] bound in Koin.
 *
 * Light or dark map style follows the current `GoZipoTheme`.
 */
@Composable
fun GoZipoMap(
    cameraState: MapCameraState,
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    showUserLocation: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMapClick: ((GeoPoint) -> Unit)? = null,
) {
    // Previews have no Koin and no map SDK
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }

    val provider = koinInject<MapProvider>()
    provider.Map(
        cameraState = cameraState,
        markers = markers,
        polylines = polylines,
        settings = MapSettings(
            darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
            showUserLocation = showUserLocation,
            contentPadding = contentPadding
        ),
        onMapClick = onMapClick,
        modifier = modifier
    )
}
