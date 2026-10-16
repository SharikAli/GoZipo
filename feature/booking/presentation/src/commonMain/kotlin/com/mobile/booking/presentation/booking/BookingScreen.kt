package com.mobile.booking.presentation.booking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.booking.presentation.components.ChooseDestinationOnMapSheet
import com.mobile.booking.presentation.components.ChooseStopsSheet
import com.mobile.booking.presentation.components.ChooseVehicleSheet
import com.mobile.booking.presentation.components.FinishedSheet
import com.mobile.booking.presentation.components.PlaceSearchOverlay
import com.mobile.booking.presentation.components.SearchingSheet
import com.mobile.booking.presentation.components.TripSheet
import com.mobile.booking.presentation.util.label
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.buttons.GoZipoIconButtonStyle
import com.mobile.core.designsystem.components.map.DestinationMarker
import com.mobile.core.designsystem.components.map.PickupMarker
import com.mobile.core.designsystem.components.map.RiderMarker
import com.mobile.core.designsystem.components.map.SearchingPulse
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.components.sheets.GoZipoMapSheetScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.map.api.GoZipoMap
import com.mobile.core.map.api.MapCameraPosition
import com.mobile.core.map.api.MapCameraState
import com.mobile.core.map.api.MapMarker
import com.mobile.core.map.api.MapPolyline
import com.mobile.core.map.api.MarkerAnchor
import com.mobile.core.map.api.rememberMapCameraState
import com.mobile.core.presentation.permissions.Permission
import com.mobile.core.presentation.permissions.rememberPermissionController
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.ride.domain.RideStatus
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.back
import gozipo.feature.booking.presentation.generated.resources.dropped_pin
import gozipo.feature.booking.presentation.generated.resources.log_out
import gozipo.feature.booking.presentation.generated.resources.menu
import gozipo.feature.booking.presentation.generated.resources.my_location
import gozipo.feature.booking.presentation.generated.resources.pickup_label
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookingRoot(
    onLogoutClick: () -> Unit,
    viewModel: BookingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraState = rememberMapCameraState()
    val permissionController = rememberPermissionController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Ride updates arrive as push notifications while the app is in the background
    LaunchedEffect(Unit) {
        permissionController.requestPermission(Permission.NOTIFICATIONS)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            BookingEvent.RequestLocationPermission -> scope.launch {
                val result = permissionController.requestPermission(Permission.LOCATION)
                viewModel.onAction(BookingAction.OnLocationPermissionResult(result))
            }

            is BookingEvent.CenterOn -> cameraState.moveTo(
                MapCameraPosition(target = event.point, zoom = MapCameraPosition.STREET_ZOOM)
            )

            is BookingEvent.FitPoints -> cameraState.fitBounds(event.points)
            is BookingEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    // The pickup pin is wherever the map settles
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isMoving to cameraState.position.target }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, target) -> target }
            .distinctUntilChanged()
            .collect { viewModel.onAction(BookingAction.OnCameraIdle(it)) }
    }

    // Frame what matters for each step of the trip
    val ride = state.ride
    LaunchedEffect(
        ride?.id,
        ride?.status,
        state.riderLocation == null
    ) {
        if (ride == null) return@LaunchedEffect
        val points = when (ride.status) {
            RideStatus.SEARCHING -> state.route?.points ?: listOf(ride.pickup.point, ride.dropoff.point)
            RideStatus.ACCEPTED, RideStatus.ARRIVED -> listOfNotNull(
                state.riderLocation,
                ride.pickup.point
            )

            else -> listOfNotNull(state.riderLocation, ride.dropoff.point)
        }
        when {
            points.size > 1 -> cameraState.fitBounds(points)
            points.size == 1 -> cameraState.moveTo(MapCameraPosition(target = points.first()))
        }
    }

    BookingScreen(
        state = state,
        cameraState = cameraState,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                BookingAction.OnLogoutClick -> onLogoutClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // GoZipoMapSheetScaffold
@Composable
fun BookingScreen(
    state: BookingState,
    cameraState: MapCameraState,
    snackbarHostState: SnackbarHostState,
    onAction: (BookingAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val stage = state.stage
    val peekHeight = stage.sheetPeekHeight()
    val topInset = if (stage == BookingStage.CHOOSE_VEHICLE) 150.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        GoZipoMapSheetScaffold(
            sheetPeekHeight = peekHeight,
            snackbarHostState = snackbarHostState,
            mapContent = {
                BookingMap(
                    state = state,
                    cameraState = cameraState,
                    contentPadding = PaddingValues(top = topInset, bottom = peekHeight),
                    onAction = onAction
                )
                when (stage) {
                    BookingStage.CHOOSE_STOPS -> CenterPin(bottomInset = peekHeight, destination = false)
                    BookingStage.CHOOSE_DESTINATION_ON_MAP -> CenterPin(bottomInset = peekHeight, destination = true)
                    else -> Unit
                }
            },
            mapOverlay = {
                MapControls(state = state, onAction = onAction)
            },
            sheetContent = {
                val modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                when (stage) {
                    BookingStage.CHOOSE_STOPS -> ChooseStopsSheet(state, onAction, modifier)
                    BookingStage.CHOOSE_DESTINATION_ON_MAP -> state.destinationPin?.let {
                        ChooseDestinationOnMapSheet(it, onAction, modifier)
                    }
                    BookingStage.CHOOSE_VEHICLE -> ChooseVehicleSheet(state, onAction, modifier)
                    BookingStage.SEARCHING -> SearchingSheet(state, onAction, modifier)
                    BookingStage.TRIP -> TripSheet(state, onAction, modifier)
                    BookingStage.FINISHED -> state.finishedRide?.let {
                        FinishedSheet(
                            it,
                            onAction,
                            modifier
                        )
                    }
                }
            }
        )

        state.search?.let { search ->
            PlaceSearchOverlay(search = search, onAction = onAction)
        }
    }
}

@Composable
private fun BookingMap(
    state: BookingState,
    cameraState: MapCameraState,
    contentPadding: PaddingValues,
    onAction: (BookingAction) -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    val stage = state.stage
    val pickup = state.pickup
    val dropoff = state.dropoff?.point

    GoZipoMap(
        cameraState = cameraState,
        modifier = Modifier.fillMaxSize(),
        markers = buildList {
            if (stage == BookingStage.SEARCHING && pickup != null) {
                add(
                    MapMarker(
                        id = "searching",
                        position = pickup,
                        anchor = MarkerAnchor.CENTER
                    ) { SearchingPulse() })
            }
            if (stage != BookingStage.CHOOSE_STOPS && pickup != null) {
                add(MapMarker(id = "pickup", position = pickup) { PickupMarker() })
            }
            if (stage != BookingStage.CHOOSE_STOPS &&
                stage != BookingStage.CHOOSE_DESTINATION_ON_MAP &&
                dropoff != null
            ) {
                add(MapMarker(id = "dropoff", position = dropoff) { DestinationMarker() })
            }
            val rider = state.riderLocation
            if (stage == BookingStage.TRIP && rider != null) {
                add(
                    MapMarker(
                        id = "rider",
                        position = rider,
                        anchor = MarkerAnchor.CENTER
                    ) { RiderMarker(bearing = 0f) })
            }
        },
        polylines = listOfNotNull(
            state.route?.takeIf {
                stage == BookingStage.CHOOSE_VEHICLE ||
                    stage == BookingStage.SEARCHING ||
                    stage == BookingStage.TRIP
            }
                ?.let { route ->
                    MapPolyline(
                        id = "route",
                        points = route.points,
                        color = extended.mapRoute,
                        casingColor = extended.mapRouteCasing
                    )
                }
        ),
        showUserLocation = state.showUserLocation,
        contentPadding = contentPadding,
        onMapClick = { onAction(BookingAction.OnMapClick(it)) }
    )
}

@Composable
private fun BoxScope.MapControls(
    state: BookingState,
    onAction: (BookingAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    when (state.stage) {
        BookingStage.CHOOSE_STOPS -> {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(spacing.lg)
            ) {
                AccountMenu(onLogoutClick = { onAction(BookingAction.OnLogoutClick) })
                Box(modifier = Modifier.weight(1f))
                RecenterButton(onAction)
            }
        }

        BookingStage.CHOOSE_VEHICLE -> {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm)
            ) {
                GoZipoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    onClick = { onAction(BookingAction.OnClearDropoffClick) },
                    style = GoZipoIconButtonStyle.MAP,
                    modifier = Modifier.padding(end = spacing.sm)
                )
                RouteStopsCard(
                    pickupText = state.pickupAddress ?: stringResource(Res.string.pickup_label),
                    destinationText = state.dropoff?.label()?.takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.dropped_pin),
                    onPickupClick = { onAction(BookingAction.OnOpenSearch(SearchTarget.PICKUP)) },
                    onDestinationClick = { onAction(BookingAction.OnOpenSearch(SearchTarget.DROPOFF)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        BookingStage.CHOOSE_DESTINATION_ON_MAP -> {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(spacing.lg)
            ) {
                GoZipoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    onClick = { onAction(BookingAction.OnCancelDestinationPinClick) },
                    style = GoZipoIconButtonStyle.MAP
                )
                Box(modifier = Modifier.weight(1f))
                RecenterButton(onAction)
            }
        }

        BookingStage.TRIP -> Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(spacing.lg)
        ) {
            RecenterButton(onAction)
        }

        BookingStage.SEARCHING, BookingStage.FINISHED -> Unit
    }
}

@Composable
private fun RecenterButton(onAction: (BookingAction) -> Unit) {
    GoZipoIconButton(
        icon = Icons.Default.MyLocation,
        contentDescription = stringResource(Res.string.my_location),
        onClick = { onAction(BookingAction.OnRecenterClick) },
        style = GoZipoIconButtonStyle.MAP
    )
}

@Composable
private fun AccountMenu(onLogoutClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GoZipoIconButton(
            icon = Icons.Default.Menu,
            contentDescription = stringResource(Res.string.menu),
            onClick = { expanded = true },
            style = GoZipoIconButtonStyle.MAP
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.log_out)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLogoutClick()
                }
            )
        }
    }
}

/**
 * Pin fixed at the camera center (the middle of the area above the sheet): the pickup, or the
 * destination while choosing it on the map.
 */
@Composable
private fun CenterPin(bottomInset: Dp, destination: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
    ) {
        // Lift the pin so its tip, not its middle, marks the center
        val liftToTip = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, -placeable.height / 2)
            }
        }
        if (destination) DestinationMarker(modifier = liftToTip) else PickupMarker(modifier = liftToTip)
    }
}

private fun BookingStage.sheetPeekHeight(): Dp = when (this) {
    BookingStage.CHOOSE_STOPS -> 220.dp
    BookingStage.CHOOSE_DESTINATION_ON_MAP -> 220.dp
    BookingStage.CHOOSE_VEHICLE -> 460.dp
    BookingStage.SEARCHING -> 400.dp
    BookingStage.TRIP -> 300.dp
    BookingStage.FINISHED -> 320.dp
}

@Composable
private fun BookingPreview(state: BookingState) {
    GoZipoTheme(darkTheme = true) {
        BookingScreen(
            state = state,
            cameraState = rememberMapCameraState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun ChooseStopsPreview() = BookingPreview(BookingPreviewData.chooseStops)

@Preview
@Composable
private fun ChooseStopsResolvingPreview() = BookingPreview(BookingPreviewData.chooseStopsResolving)

@Preview
@Composable
private fun LocationDeniedPreview() = BookingPreview(BookingPreviewData.locationDenied)

@Preview
@Composable
private fun ChooseDestinationOnMapPreview() = BookingPreview(BookingPreviewData.destinationOnMap)

@Preview
@Composable
private fun PlaceSearchPreview() = BookingPreview(BookingPreviewData.placeSearch)

@Preview
@Composable
private fun PlaceSearchNoResultsPreview() = BookingPreview(BookingPreviewData.placeSearchEmpty)

@Preview
@Composable
private fun ChooseVehicleLoadingPreview() = BookingPreview(BookingPreviewData.chooseVehicleLoading)

@Preview
@Composable
private fun ChooseVehiclePreview() = BookingPreview(BookingPreviewData.chooseVehicle)

@Preview
@Composable
private fun ChooseVehicleFailedPreview() = BookingPreview(BookingPreviewData.chooseVehicleFailed)

@Preview
@Composable
private fun SearchingPreview() = BookingPreview(BookingPreviewData.searching)

@Preview
@Composable
private fun SearchingWithOffersPreview() = BookingPreview(BookingPreviewData.searchingWithOffers)

@Preview
@Composable
private fun SearchRoundOverPreview() = BookingPreview(BookingPreviewData.searchRoundOver)

@Preview
@Composable
private fun DriverArrivingPreview() = BookingPreview(BookingPreviewData.driverArriving)

@Preview
@Composable
private fun DriverArrivedPreview() = BookingPreview(BookingPreviewData.driverArrived)

@Preview
@Composable
private fun TripInProgressPreview() = BookingPreview(BookingPreviewData.inProgress)

@Preview
@Composable
private fun RideCompletedPreview() = BookingPreview(BookingPreviewData.completed)

@Preview
@Composable
private fun RideCancelledByRiderPreview() = BookingPreview(BookingPreviewData.cancelledByRider)

@Preview
@Composable
private fun ChooseVehicleLightPreview() {
    GoZipoTheme(darkTheme = false) {
        BookingScreen(
            state = BookingPreviewData.chooseVehicle,
            cameraState = rememberMapCameraState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
