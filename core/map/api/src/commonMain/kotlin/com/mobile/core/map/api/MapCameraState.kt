package com.mobile.core.map.api

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.PaddingValues.Absolute
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.mobile.core.domain.location.GeoPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Hoisted camera of a [GoZipoMap]: read where the map is looking, and move it.
 *
 * [position] follows the camera while it moves (gestures included); [isMoving] turns false
 * when the map settles, which is the moment to reverse geocode a "pin on map" pickup.
 */
@Stable
class MapCameraState(initialPosition: MapCameraPosition) {

    companion object {
        private const val ZOOM_STEP = 1.0

        val Saver: Saver<MapCameraState, List<Double>> = Saver(
            save = {
                listOf(it.position.target.latitude, it.position.target.longitude, it.position.zoom, it.position.bearing)
            },
            restore = { (latitude, longitude, zoom, bearing) ->
                MapCameraState(MapCameraPosition(GeoPoint(latitude, longitude), zoom, bearing))
            }
        )
    }

    var position: MapCameraPosition by mutableStateOf(initialPosition)
        private set

    var isMoving: Boolean by mutableStateOf(false)
        private set

    /** True while the user can still zoom in / out (the +/- buttons). */
    val canZoomIn: Boolean get() = targetZoom < MapCameraPosition.MAX_ZOOM
    val canZoomOut: Boolean get() = targetZoom > MapCameraPosition.MIN_ZOOM

    /** Where a +/- tap is heading, so quick taps add up instead of reading a mid-animation zoom. */
    private var pendingZoom: Double? by mutableStateOf(null)
    private val targetZoom: Double get() = pendingZoom ?: position.zoom

    private val _commands = Channel<CameraCommand>(Channel.CONFLATED)

    /** Pending camera moves, consumed by the provider. */
    @MapProviderApi
    val commands: Flow<CameraCommand> = _commands.receiveAsFlow()

    fun moveTo(position: MapCameraPosition, animate: Boolean = true) {
        _commands.trySend(CameraCommand.MoveTo(position, animate))
    }

    /** One zoom level closer, keeping the same centre. */
    fun zoomIn() = zoomBy(ZOOM_STEP)

    /** One zoom level further out, keeping the same centre. */
    fun zoomOut() = zoomBy(-ZOOM_STEP)

    private fun zoomBy(delta: Double) {
        val zoom = (targetZoom + delta).coerceIn(MapCameraPosition.MIN_ZOOM, MapCameraPosition.MAX_ZOOM)
        if (zoom == targetZoom) return
        pendingZoom = zoom
        _commands.trySend(CameraCommand.MoveTo(position.copy(zoom = zoom), animate = true))
    }

    /** Zooms out or in so all [points] are visible inside [padding]. */
    fun fitBounds(
        points: List<GeoPoint>,
        padding: PaddingValues = Absolute(48.dp),
        animate: Boolean = true
    ) {
        if (points.isEmpty()) return
        _commands.trySend(CameraCommand.FitBounds(points, padding, animate))
    }

    @MapProviderApi
    fun onCameraChanged(position: MapCameraPosition, isMoving: Boolean) {
        this.position = position
        this.isMoving = isMoving
        if (!isMoving) pendingZoom = null
    }
}

sealed interface CameraCommand {
    val animate: Boolean

    data class MoveTo(
        val position: MapCameraPosition,
        override val animate: Boolean
    ) : CameraCommand

    data class FitBounds(
        val points: List<GeoPoint>,
        val padding: PaddingValues,
        override val animate: Boolean
    ) : CameraCommand
}

@Composable
fun rememberMapCameraState(
    initialPosition: MapCameraPosition = MapCameraPosition.DEFAULT
): MapCameraState = rememberSaveable(saver = MapCameraState.Saver) {
    MapCameraState(initialPosition)
}
