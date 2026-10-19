package com.mobile.driving.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.components.ride.PaymentMethodRow
import com.mobile.core.designsystem.components.ride.ProfileAvatar
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.driving.presentation.driving.DrivingAction
import com.mobile.driving.presentation.driving.DrivingState
import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.presentation.util.fareText
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.cancel_ride
import gozipo.feature.driving.presentation.generated.resources.complete_trip
import gozipo.feature.driving.presentation.generated.resources.done
import gozipo.feature.driving.presentation.generated.resources.finished_cancelled_by_customer
import gozipo.feature.driving.presentation.generated.resources.finished_cancelled_by_system
import gozipo.feature.driving.presentation.generated.resources.finished_cancelled_by_you
import gozipo.feature.driving.presentation.generated.resources.finished_collect
import gozipo.feature.driving.presentation.generated.resources.finished_completed
import gozipo.feature.driving.presentation.generated.resources.mark_arrived
import gozipo.feature.driving.presentation.generated.resources.navigate_to_destination
import gozipo.feature.driving.presentation.generated.resources.navigate_to_pickup
import gozipo.feature.driving.presentation.generated.resources.payment_cash
import gozipo.feature.driving.presentation.generated.resources.start_trip
import gozipo.feature.driving.presentation.generated.resources.trip_at_pickup
import gozipo.feature.driving.presentation.generated.resources.trip_fare
import gozipo.feature.driving.presentation.generated.resources.trip_in_progress
import gozipo.feature.driving.presentation.generated.resources.trip_to_pickup
import org.jetbrains.compose.resources.stringResource

/** Assigned to a ride: drive to the pickup, wait for the customer, then drive to the destination. */
@Composable
fun RiderTripSheet(
    state: DrivingState,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ride = state.ride ?: return
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val customerName = ride.customer.username

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(vertical = spacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        ) {
            ProfileAvatar()
            Text(
                text = when (ride.status) {
                    RideStatus.ARRIVED -> stringResource(Res.string.trip_at_pickup, customerName)
                    RideStatus.IN_PROGRESS -> stringResource(Res.string.trip_in_progress)
                    else -> stringResource(Res.string.trip_to_pickup, customerName)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary
            )
        }
        GoZipoDivider()
        PaymentMethodRow(
            label = stringResource(Res.string.trip_fare),
            amountText = fareText(ride.agreedFare ?: ride.offeredFare),
            methodText = stringResource(Res.string.payment_cash),
        )
        GoZipoDivider()
        RouteStopsCard(
            pickupText = ride.pickup.address,
            destinationText = ride.dropoff.address,
            elevated = false,
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        )

        val buttonModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal)
        // In-app turn-by-turn guidance for the current leg
        if (state.navigationTarget != null) {
            GoZipoButton(
                text = stringResource(
                    if (ride.status == RideStatus.IN_PROGRESS) Res.string.navigate_to_destination
                    else Res.string.navigate_to_pickup
                ),
                onClick = { onAction(DrivingAction.OnNavigateClick) },
                leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                style = GoZipoButtonStyle.SECONDARY,
                modifier = buttonModifier
            )
        }
        GoZipoButton(
            text = stringResource(
                when (ride.status) {
                    RideStatus.ARRIVED -> Res.string.start_trip
                    RideStatus.IN_PROGRESS -> Res.string.complete_trip
                    else -> Res.string.mark_arrived
                }
            ),
            onClick = {
                onAction(
                    when (ride.status) {
                        RideStatus.ARRIVED -> DrivingAction.OnStartTripClick
                        RideStatus.IN_PROGRESS -> DrivingAction.OnCompleteTripClick
                        else -> DrivingAction.OnArrivedClick
                    }
                )
            },
            isLoading = state.isUpdatingTrip,
            enabled = !state.isCancelling,
            modifier = buttonModifier
        )
        // The backend lets the rider cancel until the trip starts
        if (ride.status != RideStatus.IN_PROGRESS) {
            GoZipoButton(
                text = stringResource(Res.string.cancel_ride),
                onClick = { onAction(DrivingAction.OnCancelRideClick) },
                isLoading = state.isCancelling,
                enabled = !state.isUpdatingTrip,
                style = GoZipoButtonStyle.TEXT,
                modifier = buttonModifier
            )
        }
    }
}

/** The trip completed (collect the cash fare) or was cancelled. */
@Composable
fun RiderFinishedSheet(
    ride: Ride,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val completed = ride.status == RideStatus.COMPLETED

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(vertical = spacing.sm)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        ) {
            Text(
                text = stringResource(
                    when {
                        completed -> Res.string.finished_completed
                        ride.cancelledBy == CancelledBy.CUSTOMER -> Res.string.finished_cancelled_by_customer
                        ride.cancelledBy == CancelledBy.RIDER -> Res.string.finished_cancelled_by_you
                        else -> Res.string.finished_cancelled_by_system
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary
            )
            if (completed) {
                Text(
                    text = stringResource(
                        Res.string.finished_collect,
                        fareText(ride.agreedFare ?: ride.offeredFare)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = extended.cash
                )
            }
        }
        GoZipoDivider()
        RouteStopsCard(
            pickupText = ride.pickup.address,
            destinationText = ride.dropoff.address,
            elevated = false,
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
        )
        GoZipoButton(
            text = stringResource(Res.string.done),
            onClick = { onAction(DrivingAction.OnFinishedDismiss) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal)
        )
    }
}
