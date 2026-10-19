package com.mobile.core.map.navigation.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.map.api.NavigationProvider
import com.mobile.core.map.api.NavigationSettings

/** [NavigationProvider] on the Mapbox Navigation SDK for iOS, through the Swift bridge. */
internal class MapboxIosNavigationProvider(
    private val factory: GoZipoNavigationFactory,
    private val accessToken: String,
    private val simulate: Boolean,
) : NavigationProvider {

    @Composable
    override fun Navigation(
        destination: GeoPoint,
        settings: NavigationSettings,
        onArrival: () -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    ) {
        val currentOnArrival by rememberUpdatedState(onArrival)
        val currentOnClose by rememberUpdatedState(onClose)

        // The SDK's UI picks day or night when guidance starts
        val session = remember(destination) {
            factory.createNavigation(
                accessToken = accessToken,
                destination = destination.toBridge(),
                origin = settings.origin?.toBridge(),
                destinationName = settings.destinationName,
                darkTheme = settings.darkTheme,
                simulate = simulate,
                listener = object : GoZipoNavigationListener {
                    override fun onArrival() = currentOnArrival()
                    override fun onClose() = currentOnClose()
                }
            )
        }
        DisposableEffect(session) {
            onDispose { session.destroy() }
        }

        UIKitViewController(
            factory = { session.viewController() },
            modifier = modifier,
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true
            )
        )
    }

    private fun GeoPoint.toBridge() = GoZipoNavigationCoordinate(latitude, longitude)
}
