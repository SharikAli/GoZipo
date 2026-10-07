package com.mobile.apps.common.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.buttons.GoZipoIconButtonStyle
import com.mobile.core.designsystem.components.map.DestinationMarker
import com.mobile.core.designsystem.components.map.PickupMarker
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.components.sheets.GoZipoMapSheetScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.route.Route
import com.mobile.core.map.api.GoZipoMap
import com.mobile.core.map.api.MapCameraPosition
import com.mobile.core.map.api.MapCameraState
import com.mobile.core.map.api.MapMarker
import com.mobile.core.map.api.MapPolyline
import com.mobile.core.map.api.rememberMapCameraState
import com.mobile.core.presentation.permissions.Permission
import com.mobile.core.presentation.permissions.rememberPermissionController
import com.mobile.core.presentation.util.ObserveAsEvents
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes

private val SheetPeekHeight = 280.dp

/**
 * Temporary logged-in home of both apps (replaced in Phases 6 and 7).
 * Text is inline on purpose; this screen is replaced, not localized.
 */
@Composable
fun HomeMapRoot(
    onLogoutClick: () -> Unit,
    viewModel: HomeMapViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraState = rememberMapCameraState()
    val permissionController = rememberPermissionController()
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeMapEvent.RequestLocationPermission -> scope.launch {
                val result = permissionController.requestPermission(Permission.LOCATION)
                viewModel.onAction(HomeMapAction.OnLocationPermissionResult(result))
            }
            is HomeMapEvent.CenterOn -> cameraState.moveTo(
                MapCameraPosition(target = event.point, zoom = MapCameraPosition.STREET_ZOOM)
            )
            is HomeMapEvent.ShowRoute -> cameraState.fitBounds(event.points)
        }
    }

    // The pickup pin is wherever the map settles
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isMoving to cameraState.position.target }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, target) -> target }
            .distinctUntilChanged()
            .collect { viewModel.onAction(HomeMapAction.OnCameraIdle(it)) }
    }

    HomeMapScreen(
        state = state,
        cameraState = cameraState,
        onAction = { action ->
            when (action) {
                HomeMapAction.OnLogoutClick -> onLogoutClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // GoZipoMapSheetScaffold
@Composable
fun HomeMapScreen(
    state: HomeMapState,
    cameraState: MapCameraState,
    onAction: (HomeMapAction) -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    val spacing = GoZipoTheme.spacing

    GoZipoMapSheetScaffold(
        sheetPeekHeight = SheetPeekHeight,
        mapContent = {
            GoZipoMap(
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize(),
                markers = buildList {
                    val pickup = state.pickup
                    val destination = state.destination
                    if (destination != null && pickup != null) {
                        add(MapMarker(id = "pickup", position = pickup) { PickupMarker() })
                        add(MapMarker(id = "destination", position = destination.point) { DestinationMarker() })
                    }
                },
                polylines = listOfNotNull(
                    state.route?.let { route ->
                        MapPolyline(
                            id = "route",
                            points = route.points,
                            color = extended.mapRoute,
                            casingColor = extended.mapRouteCasing
                        )
                    }
                ),
                showUserLocation = state.showUserLocation,
                contentPadding = PaddingValues(bottom = SheetPeekHeight),
                onMapClick = { onAction(HomeMapAction.OnMapClick(it)) }
            )
            if (state.destination == null) {
                CenterPin(bottomInset = SheetPeekHeight)
            }
        },
        mapOverlay = {
            GoZipoIconButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "My location",
                onClick = { onAction(HomeMapAction.OnRecenterClick) },
                style = GoZipoIconButtonStyle.MAP,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(spacing.lg)
            )
        },
        sheetContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
            ) {
                RouteStopsCard(
                    pickupText = when {
                        state.isResolvingPickup -> "Finding address…"
                        else -> state.pickupAddress ?: "Move the map to set pickup"
                    },
                    pickupPlaceholder = state.pickupAddress == null,
                    destinationText = state.destination?.label() ?: "Tap the map to set a destination",
                    destinationPlaceholder = state.destination == null,
                    destinationTrailing = state.destination?.let {
                        {
                            IconButton(onClick = { onAction(HomeMapAction.OnClearDestinationClick) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear destination")
                            }
                        }
                    },
                    elevated = false
                )
                Text(
                    text = statusText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
                GoZipoButton(
                    text = "Log out",
                    onClick = { onAction(HomeMapAction.OnLogoutClick) },
                    style = GoZipoButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/** Pickup pin fixed at the camera center (the middle of the area above the sheet). */
@Composable
private fun CenterPin(bottomInset: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
    ) {
        PickupMarker(
            // Lift the pin so its tip, not its middle, marks the center
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, -placeable.height / 2)
                }
            }
        )
    }
}

private fun Place.label(): String =
    listOf(name, address).filter { it.isNotBlank() }.joinToString(", ")

private fun statusText(state: HomeMapState): String = when {
    state.locationError == LocationError.PERMISSION_DENIED -> "Location permission is off. Tap the location button to allow it."
    state.locationError == LocationError.SERVICES_DISABLED -> "Turn on location in your device settings."
    state.locationError == LocationError.UNSUPPORTED -> "Location isn't available on this device yet."
    state.isLoadingRoute -> "Finding the best route…"
    state.routeFailed -> "Couldn't get a route. Check the Mapbox token and your connection."
    state.route != null -> state.route.summary()
    else -> "Drag the map to place the pickup pin."
}

private fun Route.summary(): String {
    val km = (distanceMeters / 100) / 10.0
    val minutes = duration.inWholeMinutes.coerceAtLeast(1)
    return "$km km · about $minutes min by road"
}

@Preview
@Composable
private fun HomeMapScreenPreview() {
    GoZipoTheme(darkTheme = true) {
        HomeMapScreen(
            state = HomeMapState(
                pickup = GeoPoint(24.9425, 67.114),
                pickupAddress = "Karachi University, Karachi",
                destination = Place("1", "Dolmen Mall Clifton", "Block 4, Clifton", GeoPoint(24.80, 67.03)),
                route = Route(distanceMeters = 18_400, duration = 34.minutes, points = emptyList())
            ),
            cameraState = rememberMapCameraState(),
            onAction = {}
        )
    }
}
