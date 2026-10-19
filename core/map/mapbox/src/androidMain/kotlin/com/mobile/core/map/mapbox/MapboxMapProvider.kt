package com.mobile.core.map.mapbox

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
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

/** [MapProvider] on the Mapbox Maps SDK v11 Compose extension (Standard style). */
@OptIn(MapProviderApi::class)
class MapboxMapProvider(accessToken: String) : MapProvider {

    private companion object {
        const val ANIMATION_MILLIS = 600L
        const val CASING_WIDTH = 1.5
        // Full brightness: the Standard style's night lighting otherwise darkens annotations
        const val FULL_EMISSIVE_STRENGTH = 1.0
    }

    init {
        // Must be set before the first map is created
        MapboxOptions.accessToken = accessToken
    }

    @Composable
    override fun Map(
        cameraState: MapCameraState,
        markers: List<MapMarker>,
        polylines: List<MapPolyline>,
        settings: MapSettings,
        onMapClick: ((GeoPoint) -> Unit)?,
        modifier: Modifier
    ) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val contentInsets = settings.contentPadding.toEdgeInsets(density, layoutDirection)

        val viewportState = rememberMapViewportState {
            setCameraOptions(cameraState.position.toCameraOptions(contentInsets))
        }
        val currentContentInsets by rememberUpdatedState(contentInsets)
        LaunchedEffect(cameraState, viewportState) {
            cameraState.commands.collect { command ->
                viewportState.apply(command, currentContentInsets, density, layoutDirection)
            }
        }

        val styleState = rememberStandardStyleState()
        SideEffect {
            styleState.configurationsState.lightPreset =
                if (settings.darkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY
        }

        val currentOnMapClick by rememberUpdatedState(onMapClick)
        val clickListener = remember {
            OnMapClickListener { point ->
                currentOnMapClick?.invoke(point.toGeoPoint())
                currentOnMapClick != null
            }
        }

        // Keep the Mapbox logo and attribution (required by the terms) above app UI such as a sheet
        val ornamentPadding = PaddingValues(
            start = settings.contentPadding.calculateStartPadding(layoutDirection),
            end = settings.contentPadding.calculateEndPadding(layoutDirection),
            bottom = settings.contentPadding.calculateBottomPadding()
        )

        MapboxMap(
            modifier = modifier,
            mapViewportState = viewportState,
            compass = {},
            scaleBar = {},
            logo = { Logo(modifier = Modifier.padding(ornamentPadding)) },
            attribution = { Attribution(modifier = Modifier.padding(ornamentPadding)) },
            onMapClickListener = clickListener,
            style = { MapboxStandardStyle(standardStyleState = styleState) }
        ) {

            // Report camera moves back to the shared state
            DisposableMapEffect(cameraState) { mapView ->
                val map = mapView.mapboxMap
                val cameraChanged = map.subscribeCameraChanged {
                    cameraState.onCameraChanged(map.cameraState.toPosition(), isMoving = true)
                }
                val mapIdle = map.subscribeMapIdle {
                    cameraState.onCameraChanged(map.cameraState.toPosition(), isMoving = false)
                }
                onDispose {
                    cameraChanged.cancel()
                    mapIdle.cancel()
                }
            }

            MapEffect(settings.showUserLocation) { mapView ->
                mapView.location.updateSettings {
                    enabled = settings.showUserLocation
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                }
            }

            polylines.forEach { polyline ->
                key(polyline.id) {
                    val state = remember { PolylineAnnotationState() }
                    SideEffect {
                        state.lineColor = polyline.color
                        state.lineWidth = polyline.width.value.toDouble()
                        state.lineJoin = LineJoin.ROUND
                        state.lineEmissiveStrength = FULL_EMISSIVE_STRENGTH
                        state.lineBorderColor = polyline.casingColor
                        state.lineBorderWidth = if (polyline.casingColor != null) CASING_WIDTH else 0.0
                    }
                    PolylineAnnotation(
                        points = polyline.points.map { it.toPoint() },
                        polylineAnnotationState = state
                    )
                }
            }

            markers.forEach { marker ->
                key(marker.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(marker.position.toPoint())
                            annotationAnchor {
                                anchor(
                                    when (marker.anchor) {
                                        MarkerAnchor.BOTTOM -> ViewAnnotationAnchor.BOTTOM
                                        MarkerAnchor.CENTER -> ViewAnnotationAnchor.CENTER
                                    }
                                )
                            }
                            allowOverlap(true)
                            // The pickup is usually the user's own location
                            allowOverlapWithPuck(true)
                        }
                    ) {
                        marker.content()
                    }
                }
            }
        }
    }

    private suspend fun MapViewportState.apply(
        command: CameraCommand,
        contentInsets: EdgeInsets,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val target = when (command) {
            is CameraCommand.MoveTo -> command.position.toCameraOptions(contentInsets)
            is CameraCommand.FitBounds -> cameraForCoordinates(
                coordinates = command.points.map { it.toPoint() },
                coordinatesPadding = (command.padding.toEdgeInsets(density, layoutDirection) + contentInsets),
                maxZoom = MapCameraPosition.STREET_ZOOM
            )
        }
        if (command.animate) {
            easeTo(target, MapAnimationOptions.mapAnimationOptions { duration(ANIMATION_MILLIS) })
        } else {
            setCameraOptions(target)
        }
    }
}

private fun GeoPoint.toPoint(): Point = Point.fromLngLat(longitude, latitude)

private fun Point.toGeoPoint() = GeoPoint(latitude = latitude(), longitude = longitude())

private fun MapCameraPosition.toCameraOptions(padding: EdgeInsets): CameraOptions =
    CameraOptions.Builder()
        .center(target.toPoint())
        .zoom(zoom)
        .bearing(bearing)
        .padding(padding)
        .build()

private fun CameraState.toPosition() = MapCameraPosition(
    target = center.toGeoPoint(),
    zoom = zoom,
    bearing = bearing
)

/** EdgeInsets are in pixels. */
private fun PaddingValues.toEdgeInsets(density: Density, layoutDirection: LayoutDirection): EdgeInsets =
    with(density) {
        EdgeInsets(
            calculateTopPadding().toPx().toDouble(),
            calculateLeftPadding(layoutDirection).toPx().toDouble(),
            calculateBottomPadding().toPx().toDouble(),
            calculateRightPadding(layoutDirection).toPx().toDouble()
        )
    }

private operator fun EdgeInsets.plus(other: EdgeInsets) = EdgeInsets(
    top + other.top,
    left + other.left,
    bottom + other.bottom,
    right + other.right
)

