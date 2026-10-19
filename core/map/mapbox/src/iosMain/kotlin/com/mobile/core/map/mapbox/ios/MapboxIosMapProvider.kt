package com.mobile.core.map.mapbox.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.map.api.CameraCommand
import com.mobile.core.map.api.MapCameraPosition
import com.mobile.core.map.api.MapCameraState
import com.mobile.core.map.api.MapMarker
import com.mobile.core.map.api.MapPolyline
import com.mobile.core.map.api.MapProvider
import com.mobile.core.map.api.MapProviderApi
import com.mobile.core.map.api.MapSettings
import com.mobile.core.map.api.MarkerAnchor
import kotlin.math.roundToInt

/**
 * [MapProvider] on the Mapbox Maps iOS SDK, reached through [GoZipoMapboxFactory] (Swift).
 *
 * The native map is hosted with `UIKitView`. Markers stay Compose: they are laid out over the
 * map at the screen points the SDK reports, and re-placed on every camera change. They have no
 * pointer input, so touches fall through to the map.
 */
@OptIn(MapProviderApi::class)
class MapboxIosMapProvider(
    private val factory: GoZipoMapboxFactory,
    private val accessToken: String
) : MapProvider {

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Map(
        cameraState: MapCameraState,
        markers: List<MapMarker>,
        polylines: List<MapPolyline>,
        settings: MapSettings,
        onMapClick: ((GeoPoint) -> Unit)?,
        modifier: Modifier
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val currentCameraState by rememberUpdatedState(cameraState)
        val currentOnMapClick by rememberUpdatedState(onMapClick)
        // Bumped on every camera change; read during marker placement so only layout reruns
        val cameraVersion = remember { mutableIntStateOf(0) }

        val map = remember {
            factory.createMap(
                accessToken = accessToken,
                camera = cameraState.position.toBridge(),
                listener = object : GoZipoMapboxListener {
                    override fun onCameraChanged(camera: GoZipoMapboxCamera, isMoving: Boolean) {
                        currentCameraState.onCameraChanged(camera.toPosition(), isMoving)
                        cameraVersion.intValue++
                    }

                    override fun onMapTap(coordinate: GoZipoMapboxCoordinate) {
                        currentOnMapClick?.invoke(coordinate.toGeoPoint())
                    }
                }
            )
        }
        DisposableEffect(map) {
            onDispose { map.destroy() }
        }

        val contentInsets = settings.contentPadding.toInsets(layoutDirection)
        LaunchedEffect(map, contentInsets) { map.setContentInsets(contentInsets) }
        LaunchedEffect(map, settings.darkTheme) { map.setDarkTheme(settings.darkTheme) }
        LaunchedEffect(map, settings.showUserLocation) { map.setShowUserLocation(settings.showUserLocation) }
        LaunchedEffect(map, polylines) { map.setPolylines(polylines.map { it.toBridge() }) }
        LaunchedEffect(map, cameraState) {
            cameraState.commands.collect { command ->
                when (command) {
                    is CameraCommand.MoveTo -> map.setCamera(command.position.toBridge(), command.animate)
                    is CameraCommand.FitBounds -> map.fitBounds(
                        coordinates = command.points.map { it.toBridge() },
                        padding = command.padding.toInsets(layoutDirection),
                        maxZoom = MapCameraPosition.STREET_ZOOM,
                        animated = command.animate
                    )
                }
            }
        }

        Box(modifier = modifier) {
            UIKitView(
                factory = { map.view() },
                modifier = Modifier.fillMaxSize(),
                properties = UIKitInteropProperties(
                    // Pan and pinch go straight to the map, without Compose's touch delay
                    interactionMode = UIKitInteropInteractionMode.NonCooperative,
                    isNativeAccessibilityEnabled = false
                )
            )

            Layout(
                content = {
                    markers.forEach { marker ->
                        key(marker.id) { marker.content() }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { measurables, constraints ->
                val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measurables.map { it.measure(looseConstraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    cameraVersion.intValue
                    placeables.forEachIndexed { index, placeable ->
                        val marker = markers.getOrNull(index) ?: return@forEachIndexed
                        // Points and Compose pixels differ by the screen scale (density)
                        val point = map.screenPoint(marker.position.toBridge())
                        val x = (point.x * density).roundToInt() - placeable.width / 2
                        val y = (point.y * density).roundToInt() - when (marker.anchor) {
                            MarkerAnchor.BOTTOM -> placeable.height
                            MarkerAnchor.CENTER -> placeable.height / 2
                        }
                        placeable.place(x, y)
                    }
                }
            }
        }
    }
}

private fun GeoPoint.toBridge() = GoZipoMapboxCoordinate(latitude = latitude, longitude = longitude)

private fun GoZipoMapboxCoordinate.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

private fun MapCameraPosition.toBridge() = GoZipoMapboxCamera(
    center = target.toBridge(),
    zoom = zoom,
    bearing = bearing
)

private fun GoZipoMapboxCamera.toPosition() = MapCameraPosition(
    target = center.toGeoPoint(),
    zoom = zoom,
    bearing = bearing
)

private fun MapPolyline.toBridge() = GoZipoMapboxPolyline(
    id = id,
    coordinates = points.map { it.toBridge() },
    color = color.toArgb(),
    width = width.value.toDouble(),
    casingColor = casingColor?.toArgb()
)

/** On iOS one dp is one point, so dp values pass through unchanged. */
private fun PaddingValues.toInsets(layoutDirection: LayoutDirection) = GoZipoMapboxInsets(
    top = calculateTopPadding().value.toDouble(),
    left = calculateLeftPadding(layoutDirection).value.toDouble(),
    bottom = calculateBottomPadding().value.toDouble(),
    right = calculateRightPadding(layoutDirection).value.toDouble()
)
