package com.mobile.core.map.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import com.mobile.core.domain.location.GeoPoint
import org.koin.compose.koinInject

/**
 * Full-screen, in-app turn-by-turn navigation to [destination], so drivers don't leave the
 * app for the device's maps app. The SDK comes from the [NavigationProvider] bound in Koin;
 * only apps that bind one may call this. Light or dark style follows the current `GoZipoTheme`.
 *
 * Needs location permission (the caller already has it while sharing location).
 */
@Composable
fun GoZipoNavigation(
    destination: GeoPoint,
    onArrival: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    origin: GeoPoint? = null,
    destinationName: String? = null,
) {
    // Previews have no Koin and no navigation SDK
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }

    val provider = koinInject<NavigationProvider>()
    provider.Navigation(
        destination = destination,
        settings = NavigationSettings(
            darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
            origin = origin,
            destinationName = destinationName
        ),
        onArrival = onArrival,
        onClose = onClose,
        modifier = modifier
    )
}
