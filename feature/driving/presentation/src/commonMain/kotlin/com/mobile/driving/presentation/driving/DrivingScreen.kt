package com.mobile.driving.presentation.driving

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoIconButton
import com.mobile.core.designsystem.components.buttons.GoZipoIconButtonStyle
import com.mobile.core.designsystem.components.map.DestinationMarker
import com.mobile.core.designsystem.components.map.PickupMarker
import com.mobile.core.designsystem.components.sheets.GoZipoMapSheetScaffold
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.map.api.GoZipoMap
import com.mobile.core.map.api.MapCameraPosition
import com.mobile.core.map.api.MapCameraState
import com.mobile.core.map.api.MapMarker
import com.mobile.core.map.api.MapPolyline
import com.mobile.core.map.api.rememberMapCameraState
import com.mobile.core.presentation.permissions.Permission
import com.mobile.core.presentation.permissions.rememberPermissionController
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.driving.presentation.components.NoVehicleSheet
import com.mobile.driving.presentation.components.OfflineSheet
import com.mobile.driving.presentation.components.OnlineSheet
import com.mobile.driving.presentation.components.RiderFinishedSheet
import com.mobile.driving.presentation.components.RiderTripSheet
import com.mobile.driving.presentation.components.StatusLoadingSheet
import com.mobile.driving.presentation.navigation.NavigationTarget
import com.mobile.ride.domain.RideStatus
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.log_out
import gozipo.feature.driving.presentation.generated.resources.menu
import gozipo.feature.driving.presentation.generated.resources.menu_earnings
import gozipo.feature.driving.presentation.generated.resources.menu_vehicle
import gozipo.feature.driving.presentation.generated.resources.my_location
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DrivingRoot(
    onLogoutClick: () -> Unit,
    onVehicleClick: () -> Unit,
    onEarningsClick: () -> Unit,
    onNavigateClick: (NavigationTarget) -> Unit,
    viewModel: DrivingViewModel = koinViewModel()
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
            DrivingEvent.RequestLocationPermission -> scope.launch {
                val result = permissionController.requestPermission(Permission.LOCATION)
                viewModel.onAction(DrivingAction.OnLocationPermissionResult(result))
            }

            DrivingEvent.OpenVehicleSetup -> onVehicleClick()
            is DrivingEvent.CenterOn -> cameraState.moveTo(
                MapCameraPosition(target = event.point, zoom = MapCameraPosition.STREET_ZOOM)
            )

            is DrivingEvent.FitPoints -> cameraState.fitBounds(event.points)
            is DrivingEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    // Also runs when coming back from the vehicle screen, which may have changed the status
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(DrivingAction.OnResume)
    }

    DrivingScreen(
        state = state,
        cameraState = cameraState,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                DrivingAction.OnLogoutClick -> onLogoutClick()
                DrivingAction.OnVehicleClick -> onVehicleClick()
                DrivingAction.OnEarningsClick -> onEarningsClick()
                DrivingAction.OnNavigateClick -> state.navigationTarget?.let(onNavigateClick)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // GoZipoMapSheetScaffold
@Composable
fun DrivingScreen(
    state: DrivingState,
    cameraState: MapCameraState,
    snackbarHostState: SnackbarHostState,
    onAction: (DrivingAction) -> Unit,
) {
    val stage = state.stage
    val peekHeight = stage.sheetPeekHeight()

    GoZipoMapSheetScaffold(
        sheetPeekHeight = peekHeight,
        snackbarHostState = snackbarHostState,
        mapContent = {
            DrivingMap(
                state = state,
                cameraState = cameraState,
                contentPadding = PaddingValues(bottom = peekHeight)
            )
        },
        mapOverlay = {
            MapControls(state = state, onAction = onAction)
        },
        sheetContent = {
            val modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
            when (stage) {
                DrivingStage.LOADING -> StatusLoadingSheet(state, onAction, modifier)
                DrivingStage.NO_VEHICLE -> NoVehicleSheet(onAction, modifier)
                DrivingStage.OFFLINE -> OfflineSheet(state, onAction, modifier)
                DrivingStage.ONLINE -> OnlineSheet(state, onAction, modifier)
                DrivingStage.TRIP -> RiderTripSheet(state, onAction, modifier)
                DrivingStage.FINISHED -> state.finishedRide?.let {
                    RiderFinishedSheet(
                        it,
                        onAction,
                        modifier
                    )
                }
            }
        }
    )
}

@Composable
private fun DrivingMap(
    state: DrivingState,
    cameraState: MapCameraState,
    contentPadding: PaddingValues,
) {
    val extended = MaterialTheme.colorScheme.extended
    val ride = state.ride
    val selected = state.selectedRequest

    GoZipoMap(
        cameraState = cameraState,
        modifier = Modifier.fillMaxSize(),
        markers = buildList {
            when {
                ride != null -> {
                    if (ride.status == RideStatus.IN_PROGRESS) {
                        add(
                            MapMarker(
                                id = "dropoff",
                                position = ride.dropoff.point
                            ) { DestinationMarker() })
                    } else {
                        add(
                            MapMarker(
                                id = "pickup",
                                position = ride.pickup.point
                            ) { PickupMarker() })
                    }
                }

                state.stage == DrivingStage.ONLINE -> {
                    // Every nearby pickup; the selected request also shows where it goes
                    state.requests.forEach { request ->
                        add(
                            MapMarker(
                                id = "pickup-${request.rideId}",
                                position = request.pickup.point
                            ) {
                                PickupMarker()
                            })
                    }
                    if (selected != null) {
                        add(
                            MapMarker(
                                id = "dropoff",
                                position = selected.dropoff.point
                            ) { DestinationMarker() })
                    }
                }
            }
        },
        polylines = listOfNotNull(
            state.route?.takeIf { ride != null }?.let { route ->
                MapPolyline(
                    id = "route",
                    points = route.points,
                    color = extended.mapRoute,
                    casingColor = extended.mapRouteCasing
                )
            }
        ),
        showUserLocation = state.userLocation != null,
        contentPadding = contentPadding,
    )
}

@Composable
private fun BoxScope.MapControls(
    state: DrivingState,
    onAction: (DrivingAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(spacing.lg)
    ) {
        AccountMenu(
            // Changing the vehicle mid-ride would change what the customer sees
            canEditVehicle = state.ride == null,
            onAction = onAction
        )
        Box(modifier = Modifier.weight(1f))
        GoZipoIconButton(
            icon = Icons.Default.MyLocation,
            contentDescription = stringResource(Res.string.my_location),
            onClick = { onAction(DrivingAction.OnRecenterClick) },
            style = GoZipoIconButtonStyle.MAP
        )
    }
}

@Composable
private fun AccountMenu(
    canEditVehicle: Boolean,
    onAction: (DrivingAction) -> Unit,
) {
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
                text = { Text(stringResource(Res.string.menu_vehicle)) },
                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                enabled = canEditVehicle,
                onClick = {
                    expanded = false
                    onAction(DrivingAction.OnVehicleClick)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_earnings)) },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAction(DrivingAction.OnEarningsClick)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.log_out)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAction(DrivingAction.OnLogoutClick)
                }
            )
        }
    }
}

private fun DrivingStage.sheetPeekHeight(): Dp = when (this) {
    DrivingStage.LOADING -> 160.dp
    DrivingStage.NO_VEHICLE -> 240.dp
    DrivingStage.OFFLINE -> 320.dp
    DrivingStage.ONLINE -> 420.dp
    DrivingStage.TRIP -> 380.dp
    DrivingStage.FINISHED -> 300.dp
}

@Composable
private fun DrivingPreview(state: DrivingState) {
    GoZipoTheme(darkTheme = true) {
        DrivingScreen(
            state = state,
            cameraState = rememberMapCameraState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() = DrivingPreview(DrivingPreviewData.loading)

@Preview
@Composable
private fun NoVehiclePreview() = DrivingPreview(DrivingPreviewData.noVehicle)

@Preview
@Composable
private fun OfflinePreview() = DrivingPreview(DrivingPreviewData.offline)

@Preview
@Composable
private fun OfflineLocationDeniedPreview() =
    DrivingPreview(DrivingPreviewData.offlineLocationDenied)

@Preview
@Composable
private fun OnlineWaitingPreview() = DrivingPreview(DrivingPreviewData.onlineWaiting)

@Preview
@Composable
private fun OnlineRequestsPreview() = DrivingPreview(DrivingPreviewData.onlineRequests)

@Preview
@Composable
private fun OnlineCounterPreview() = DrivingPreview(DrivingPreviewData.onlineCounter)

@Preview
@Composable
private fun OnlineOfferSentPreview() = DrivingPreview(DrivingPreviewData.onlineOfferSent)

@Preview
@Composable
private fun ToPickupPreview() = DrivingPreview(DrivingPreviewData.toPickup)

@Preview
@Composable
private fun AtPickupPreview() = DrivingPreview(DrivingPreviewData.atPickup)

@Preview
@Composable
private fun InProgressPreview() = DrivingPreview(DrivingPreviewData.inProgress)

@Preview
@Composable
private fun CompletedPreview() = DrivingPreview(DrivingPreviewData.completed)

@Preview
@Composable
private fun CancelledByCustomerPreview() = DrivingPreview(DrivingPreviewData.cancelledByCustomer)

@Preview
@Composable
private fun OnlineRequestsLightPreview() {
    GoZipoTheme(darkTheme = false) {
        DrivingScreen(
            state = DrivingPreviewData.onlineRequests,
            cameraState = rememberMapCameraState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
