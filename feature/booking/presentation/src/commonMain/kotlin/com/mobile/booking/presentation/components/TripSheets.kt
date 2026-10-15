package com.mobile.booking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.BookingState
import com.mobile.ride.presentation.util.description
import com.mobile.ride.presentation.util.formatAmount
import com.mobile.ride.presentation.util.toUi
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.ride.DefaultVehicleImage
import com.mobile.core.designsystem.components.ride.DriverSummary
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.components.ride.PaymentMethodRow
import com.mobile.core.designsystem.components.ride.ProfileAvatar
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.components.ride.SectionLabel
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideStatus
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.cancel_ride
import gozipo.feature.booking.presentation.generated.resources.done
import gozipo.feature.booking.presentation.generated.resources.driver_label
import gozipo.feature.booking.presentation.generated.resources.fare_amount
import gozipo.feature.booking.presentation.generated.resources.find_another_ride
import gozipo.feature.booking.presentation.generated.resources.payment
import gozipo.feature.booking.presentation.generated.resources.payment_cash
import gozipo.feature.booking.presentation.generated.resources.trip_cancelled_by_customer
import gozipo.feature.booking.presentation.generated.resources.trip_cancelled_by_rider
import gozipo.feature.booking.presentation.generated.resources.trip_cancelled_by_system
import gozipo.feature.booking.presentation.generated.resources.trip_completed_pay
import gozipo.feature.booking.presentation.generated.resources.trip_completed_title
import gozipo.feature.booking.presentation.generated.resources.trip_driver_arrived
import gozipo.feature.booking.presentation.generated.resources.trip_driver_arriving
import gozipo.feature.booking.presentation.generated.resources.trip_in_progress
import gozipo.feature.booking.presentation.generated.resources.trip_your_current_ride
import gozipo.feature.booking.presentation.generated.resources.vehicle_unknown
import org.jetbrains.compose.resources.stringResource

/** A rider is assigned: arriving, arrived, or driving to the destination. */
@Composable
fun TripSheet(
    state: BookingState,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ride = state.ride ?: return
    val rider = ride.rider
    val vehicle = rider?.vehicle
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier.padding(vertical = spacing.sm)
    ) {
        DriverSummary(
            title = stringResource(
                when (ride.status) {
                    RideStatus.ARRIVED -> Res.string.trip_driver_arrived
                    RideStatus.IN_PROGRESS -> Res.string.trip_in_progress
                    else -> Res.string.trip_driver_arriving
                }
            ),
            vehicleText = vehicle?.description() ?: stringResource(Res.string.vehicle_unknown),
            plateText = vehicle?.plateNumber ?: "—",
            vehicleImage = { DefaultVehicleImage(icon = ride.vehicleType.toUi().icon) }
        )
        GoZipoDivider()
        if (rider != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal)
            ) {
                ProfileAvatar()
                Column {
                    Text(
                        text = rider.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = extended.textPrimary
                    )
                    Text(
                        text = stringResource(Res.string.driver_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                }
            }
            GoZipoDivider()
        }
        PaymentMethodRow(
            label = stringResource(Res.string.payment),
            amountText = stringResource(
                Res.string.fare_amount,
                formatAmount(ride.agreedFare ?: ride.offeredFare)
            ),
            methodText = stringResource(Res.string.payment_cash),
        )
        GoZipoDivider()
        RideStops(ride = ride)
        if (ride.status != RideStatus.IN_PROGRESS) {
            GoZipoButton(
                text = stringResource(Res.string.cancel_ride),
                onClick = { onAction(BookingAction.OnCancelRideClick) },
                isLoading = state.isCancelling,
                style = GoZipoButtonStyle.DESTRUCTIVE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontal)
            )
        }
    }
}

/** The trip finished, or someone other than the customer canceled it. */
@Composable
fun FinishedSheet(
    ride: Ride,
    onAction: (BookingAction) -> Unit,
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
                        completed -> Res.string.trip_completed_title
                        ride.cancelledBy == CancelledBy.RIDER -> Res.string.trip_cancelled_by_rider
                        ride.cancelledBy == CancelledBy.CUSTOMER -> Res.string.trip_cancelled_by_customer
                        else -> Res.string.trip_cancelled_by_system
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = extended.textPrimary
            )
            if (completed) {
                Text(
                    text = stringResource(
                        Res.string.trip_completed_pay,
                        stringResource(
                            Res.string.fare_amount,
                            formatAmount(ride.agreedFare ?: ride.offeredFare)
                        )
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = extended.textSecondary
                )
            }
        }
        GoZipoDivider()
        RideStops(ride = ride)
        GoZipoButton(
            text = stringResource(if (completed) Res.string.done else Res.string.find_another_ride),
            onClick = { onAction(BookingAction.OnFinishedDismiss) },
            style = if (completed) GoZipoButtonStyle.PRIMARY else GoZipoButtonStyle.SECONDARY,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal)
        )
    }
}

@Composable
private fun RideStops(ride: Ride) {
    val spacing = GoZipoTheme.spacing
    Column(modifier = Modifier.padding(horizontal = spacing.screenHorizontal)) {
        SectionLabel(text = stringResource(Res.string.trip_your_current_ride))
        RouteStopsCard(
            pickupText = ride.pickup.address,
            destinationText = ride.dropoff.address,
            elevated = false
        )
    }
}
