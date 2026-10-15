package com.mobile.driving.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.cards.GoZipoCard
import com.mobile.core.designsystem.components.ride.FareStepper
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.driving.presentation.driving.DrivingAction
import com.mobile.driving.presentation.driving.DrivingPreviewData
import com.mobile.ride.domain.OfferStatus
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.presentation.util.distanceText
import com.mobile.ride.presentation.util.fareText
import com.mobile.ride.presentation.util.minutesText
import com.mobile.ride.presentation.util.rememberRemaining
import com.mobile.ride.presentation.util.toCountdownText
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.accept_for
import gozipo.feature.driving.presentation.generated.resources.fare_decrease
import gozipo.feature.driving.presentation.generated.resources.fare_increase
import gozipo.feature.driving.presentation.generated.resources.offer_amount
import gozipo.feature.driving.presentation.generated.resources.offer_sent
import gozipo.feature.driving.presentation.generated.resources.offer_waiting
import gozipo.feature.driving.presentation.generated.resources.request_customer_fare
import gozipo.feature.driving.presentation.generated.resources.request_pickup_away
import gozipo.feature.driving.presentation.generated.resources.request_trip_summary
import gozipo.feature.driving.presentation.generated.resources.request_your_offer
import gozipo.feature.driving.presentation.generated.resources.withdraw_offer
import org.jetbrains.compose.resources.stringResource

/**
 * One open request: the customer's fare, how far the pickup is, the trip, and time left in the
 * round. Selected, it shows the offer stepper (the fare as-is, or a counter up to the backend's
 * limit); once offered, the pending offer and a withdraw button.
 */
@Composable
fun RequestCard(
    request: OpenRide,
    selected: Boolean,
    offerAmount: Int?,
    offerRange: IntRange?,
    isSubmitting: Boolean,
    isWithdrawing: Boolean,
    onAction: (DrivingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val remaining = rememberRemaining(request.searchExpiresAt)
    val myOffer = request.myOffer?.takeIf { it.status == OfferStatus.PENDING }

    GoZipoCard(
        selected = selected,
        onClick = { onAction(DrivingAction.OnRequestClick(request.rideId)) },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fareText(request.offeredFare),
                        style = MaterialTheme.typography.headlineSmall,
                        color = extended.textPrimary
                    )
                    Text(
                        text = request.customer.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                }
                if (request.searchExpiresAt != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = extended.surfaceHighest,
                        contentColor = extended.textPrimary
                    ) {
                        Text(
                            text = remaining.toCountdownText(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = spacing.sm,
                                vertical = spacing.xs
                            )
                        )
                    }
                }
            }

            val pickupDistance = request.pickupDistanceMeters
            Text(
                text = listOfNotNull(
                    pickupDistance?.let {
                        stringResource(
                            Res.string.request_pickup_away,
                            distanceText(it)
                        )
                    },
                    stringResource(
                        Res.string.request_trip_summary,
                        distanceText(request.distanceMeters),
                        minutesText(request.duration)
                    )
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
            RouteStopsCard(
                pickupText = request.pickup.address,
                destinationText = request.dropoff.address,
                elevated = false
            )

            when {
                myOffer != null -> PendingOffer(
                    amount = myOffer.amount,
                    isWithdrawing = isWithdrawing,
                    onWithdraw = {
                        onAction(
                            DrivingAction.OnWithdrawOfferClick(
                                request.rideId,
                                myOffer.id
                            )
                        )
                    }
                )

                selected && offerAmount != null -> OfferControls(
                    request = request,
                    amount = offerAmount,
                    range = offerRange,
                    isSubmitting = isSubmitting,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun OfferControls(
    request: OpenRide,
    amount: Int,
    range: IntRange?,
    isSubmitting: Boolean,
    onAction: (DrivingAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val isCounter = amount != request.offeredFare
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        FareStepper(
            fareText = fareText(amount),
            caption = stringResource(if (isCounter) Res.string.request_your_offer else Res.string.request_customer_fare),
            onDecrease = { onAction(DrivingAction.OnDecreaseOfferClick) },
            onIncrease = { onAction(DrivingAction.OnIncreaseOfferClick) },
            canDecrease = range != null && amount - request.fareStep >= range.first,
            canIncrease = range != null && amount + request.fareStep <= range.last,
            decreaseContentDescription = stringResource(Res.string.fare_decrease),
            increaseContentDescription = stringResource(Res.string.fare_increase),
        )
        GoZipoButton(
            text = stringResource(
                if (isCounter) Res.string.offer_amount else Res.string.accept_for,
                fareText(amount)
            ),
            onClick = { onAction(DrivingAction.OnSubmitOfferClick) },
            isLoading = isSubmitting,
            style = if (isCounter) GoZipoButtonStyle.SECONDARY else GoZipoButtonStyle.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PendingOffer(
    amount: Int,
    isWithdrawing: Boolean,
    onWithdraw: () -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.offer_sent, fareText(amount)),
                style = MaterialTheme.typography.titleMedium,
                color = extended.textPrimary
            )
            Text(
                text = stringResource(Res.string.offer_waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary
            )
        }
        GoZipoButton(
            text = stringResource(Res.string.withdraw_offer),
            onClick = onWithdraw,
            isLoading = isWithdrawing,
            style = GoZipoButtonStyle.TEXT
        )
    }
}

@Preview
@Composable
private fun RequestCardPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.md),
                modifier = Modifier.padding(GoZipoTheme.spacing.lg)
            ) {
                val requests = DrivingPreviewData.requests
                RequestCard(requests[0], true, 600, 540..810, false, false, {})
                RequestCard(requests[1], false, null, null, false, false, {})
            }
        }
    }
}
