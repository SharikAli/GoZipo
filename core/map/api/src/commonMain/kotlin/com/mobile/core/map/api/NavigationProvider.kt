package com.mobile.core.map.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.mobile.core.domain.location.GeoPoint

/**
 * In-app turn-by-turn guidance behind [GoZipoNavigation]. Today the Mapbox Navigation SDK
 * (:core:map:navigation), bound only in the rider app. Features never see the implementation.
 *
 * Implementations route from the device's location (or [NavigationSettings.origin] until the
 * first fix) to the destination, show maneuvers, remaining time and distance, speak
 * instructions, reroute when the driver leaves the route, and call `onArrival` once at the
 * destination. `onClose` is the user ending guidance themselves.
 */
interface NavigationProvider {
    @Composable
    fun Navigation(
        destination: GeoPoint,
        settings: NavigationSettings,
        onArrival: () -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    )
}

/**
 * @param origin where to start the route before the SDK has its own location fix
 * @param destinationName shown by providers that label the destination
 */
@Immutable
data class NavigationSettings(
    val darkTheme: Boolean,
    val origin: GeoPoint? = null,
    val destinationName: String? = null,
)
