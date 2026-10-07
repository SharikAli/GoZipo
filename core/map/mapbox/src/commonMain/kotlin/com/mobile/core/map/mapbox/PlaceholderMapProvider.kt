package com.mobile.core.map.mapbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.map.api.MapCameraState
import com.mobile.core.map.api.MapMarker
import com.mobile.core.map.api.MapPolyline
import com.mobile.core.map.api.MapProvider
import com.mobile.core.map.api.MapSettings

/**
 * Stands in for the map where Mapbox can't run: on iOS until the Mapbox iOS SDK is bridged,
 * and on Android when MAPBOX_ACCESS_TOKEN is missing (the SDK would throw).
 */
class PlaceholderMapProvider(private val message: String) : MapProvider {
    @Composable
    override fun Map(
        cameraState: MapCameraState,
        markers: List<MapMarker>,
        polylines: List<MapPolyline>,
        settings: MapSettings,
        onMapClick: ((GeoPoint) -> Unit)?,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
