package com.mobile.driving.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.ride.DefaultVehicleImage
import com.mobile.core.designsystem.components.ride.DriverSummary
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.components.ride.SectionLabel
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.domain.location.LocationError
import com.mobile.driving.presentation.driving.DrivingAction
import com.mobile.driving.presentation.driving.DrivingState
import com.mobile.ride.presentation.util.description
import com.mobile.ride.presentation.util.toUi
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.add_vehicle
import gozipo.feature.driving.presentation.generated.resources.go_offline
import gozipo.feature.driving.presentation.generated.resources.go_online
import gozipo.feature.driving.presentation.generated.resources.location_needed
import gozipo.feature.driving.presentation.generated.resources.location_services_disabled
import gozipo.feature.driving.presentation.generated.resources.location_unavailable
import gozipo.feature.driving.presentation.generated.resources.no_vehicle_hint
import gozipo.feature.driving.presentation.generated.resources.no_vehicle_title
import gozipo.feature.driving.presentation.generated.resources.requests_title
import gozipo.feature.driving.presentation.generated.resources.retry
import gozipo.feature.driving.presentation.generated.resources.status_offline
import gozipo.feature.driving.presentation.generated.resources.status_offline_hint
import gozipo.feature.driving.presentation.generated.resources.status_online
import gozipo.feature.driving.presentation.generated.resources.status_online_hint
import gozipo.feature.driving.presentation.generated.resources.waiting_for_requests
import gozipo.feature.driving.presentation.generated.resources.status_failed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** First load of the rider's status, or it failed. */
@Composable
fun StatusLoadingSheet(
    state: DrivingState,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(spacing.xl)
    ) {
        if (state.statusFailed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Text(
                    text = stringResource(Res.string.status_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.extended.textSecondary
                )
                GoZipoButton(
                    text = stringResource(Res.string.retry),
                    onClick = { onAction(DrivingAction.OnRetryStatusClick) },
                    style = GoZipoButtonStyle.SECONDARY
                )
            }
        } else {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.extended.textPrimary)
        }
    }
}

/** The rider has no vehicle yet; it's required before going online. */
@Composable
fun NoVehicleSheet(
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        SheetHeader(
            title = stringResource(Res.string.no_vehicle_title),
            subtitle = stringResource(Res.string.no_vehicle_hint)
        )
        GoZipoButton(
            text = stringResource(Res.string.add_vehicle),
            onClick = { onAction(DrivingAction.OnVehicleClick) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OfflineSheet(
    state: DrivingState,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(vertical = spacing.sm)
    ) {
        SheetHeader(
            title = stringResource(Res.string.status_offline),
            subtitle = stringResource(Res.string.status_offline_hint),
            online = false,
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        )
        state.vehicle?.let { vehicle ->
            GoZipoDivider()
            DriverSummary(
                title = stringResource(vehicle.type.toUi().title),
                vehicleText = vehicle.description(),
                plateText = vehicle.plateNumber,
                vehicleImage = { DefaultVehicleImage(icon = vehicle.type.toUi().icon) }
            )
            GoZipoDivider()
        }
        state.locationError?.toMessage()?.let { message ->
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
            )
        }
        GoZipoButton(
            text = stringResource(Res.string.go_online),
            onClick = { onAction(DrivingAction.OnGoOnlineClick) },
            isLoading = state.isChangingAvailability,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal)
        )
    }
}

/** Online without a ride: the live list of requests near the rider. */
@Composable
fun OnlineSheet(
    state: DrivingState,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetHeader(
                title = stringResource(Res.string.status_online),
                subtitle = stringResource(Res.string.status_online_hint),
                online = true,
                modifier = Modifier.weight(1f)
            )
            GoZipoButton(
                text = stringResource(Res.string.go_offline),
                onClick = { onAction(DrivingAction.OnGoOfflineClick) },
                isLoading = state.isChangingAvailability,
                style = GoZipoButtonStyle.SECONDARY
            )
        }

        if (state.requests.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.padding(vertical = spacing.lg)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.extended.textSecondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.waiting_for_requests),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.extended.textSecondary
                )
            }
        } else {
            SectionLabel(text = stringResource(Res.string.requests_title))
            state.requests.forEach { request ->
                val selected = request.rideId == state.selectedRideId
                RequestCard(
                    request = request,
                    selected = selected,
                    offerAmount = if (selected) state.offerAmount else null,
                    offerRange = if (selected) state.offerRange else null,
                    isSubmitting = state.submittingRideId == request.rideId,
                    isWithdrawing = request.myOffer?.id != null && state.withdrawingOfferId == request.myOffer?.id,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
internal fun SheetHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    online: Boolean? = null,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (online != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (online) extended.success else extended.textTertiary)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary
        )
    }
}

private fun LocationError.toMessage(): StringResource? = when (this) {
    LocationError.PERMISSION_DENIED -> Res.string.location_needed
    LocationError.SERVICES_DISABLED -> Res.string.location_services_disabled
    LocationError.UNAVAILABLE -> Res.string.location_unavailable
    LocationError.UNSUPPORTED -> null
}
