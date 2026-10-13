package com.mobile.booking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.BookingState
import com.mobile.booking.presentation.util.description
import com.mobile.booking.presentation.util.formatAmount
import com.mobile.booking.presentation.util.formatKilometers
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.ride.CountdownHeader
import com.mobile.core.designsystem.components.ride.FareStepper
import com.mobile.core.designsystem.components.ride.ProfileAvatar
import com.mobile.core.designsystem.components.ride.SectionLabel
import com.mobile.core.designsystem.components.ride.VerifiedLabel
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.ride.domain.OfferKind
import com.mobile.ride.domain.RideOffer
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.accept
import gozipo.feature.booking.presentation.generated.resources.all_drivers_verified
import gozipo.feature.booking.presentation.generated.resources.cancel_search
import gozipo.feature.booking.presentation.generated.resources.decline
import gozipo.feature.booking.presentation.generated.resources.distance_kilometers
import gozipo.feature.booking.presentation.generated.resources.distance_meters
import gozipo.feature.booking.presentation.generated.resources.fare_amount
import gozipo.feature.booking.presentation.generated.resources.fare_decrease
import gozipo.feature.booking.presentation.generated.resources.fare_increase
import gozipo.feature.booking.presentation.generated.resources.fare_your_offer
import gozipo.feature.booking.presentation.generated.resources.offer_accepts_fare
import gozipo.feature.booking.presentation.generated.resources.offer_counter
import gozipo.feature.booking.presentation.generated.resources.offer_distance_away
import gozipo.feature.booking.presentation.generated.resources.offers_title
import gozipo.feature.booking.presentation.generated.resources.raise_fare
import gozipo.feature.booking.presentation.generated.resources.search_again
import gozipo.feature.booking.presentation.generated.resources.searching_round_ended
import gozipo.feature.booking.presentation.generated.resources.searching_round_ended_hint
import gozipo.feature.booking.presentation.generated.resources.searching_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val SEARCH_ROUND = 60.seconds

/** "Searching for drivers 0:58", the fare to raise, and the riders' offers as they arrive. */
@Composable
fun SearchingSheet(
    state: BookingState,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ride = state.ride ?: return
    val spacing = GoZipoTheme.spacing
    val remaining = rememberRemaining(ride.searchExpiresAt)

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        CountdownHeader(
            title = stringResource(
                if (state.isSearchRoundOver) Res.string.searching_round_ended else Res.string.searching_title
            ),
            timeText = remaining.toCountdownText(),
            progress = { (remaining / SEARCH_ROUND).toFloat().coerceIn(0f, 1f) },
            subtitle = {
                if (state.isSearchRoundOver) {
                    Text(
                        text = stringResource(Res.string.searching_round_ended_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.extended.textSecondary
                    )
                } else {
                    VerifiedLabel(text = stringResource(Res.string.all_drivers_verified))
                }
            }
        )

        val fare = state.offeredFare ?: ride.offeredFare
        val range = state.fareRange
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.extended.surfaceHigher,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.padding(spacing.md)
            ) {
                FareStepper(
                    fareText = stringResource(Res.string.fare_amount, formatAmount(fare)),
                    caption = stringResource(Res.string.fare_your_offer),
                    onDecrease = { onAction(BookingAction.OnDecreaseFareClick) },
                    onIncrease = { onAction(BookingAction.OnIncreaseFareClick) },
                    canDecrease = range != null && fare - state.fareStep >= range.first,
                    canIncrease = range != null && fare + state.fareStep <= range.last,
                    decreaseContentDescription = stringResource(Res.string.fare_decrease),
                    increaseContentDescription = stringResource(Res.string.fare_increase),
                )
                GoZipoButton(
                    text = stringResource(
                        if (state.isSearchRoundOver && fare == ride.offeredFare) Res.string.search_again
                        else Res.string.raise_fare
                    ),
                    onClick = { onAction(BookingAction.OnConfirmFareClick) },
                    enabled = state.canConfirmFare,
                    isLoading = state.isUpdatingFare,
                    style = if (state.isSearchRoundOver) GoZipoButtonStyle.PRIMARY else GoZipoButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.offers.isNotEmpty()) {
            SectionLabel(text = stringResource(Res.string.offers_title))
            state.offers.forEach { offer ->
                OfferCard(
                    offer = offer,
                    isAccepting = state.acceptingOfferId == offer.id,
                    acceptEnabled = state.acceptingOfferId == null && !state.isSearchRoundOver,
                    onAccept = { onAction(BookingAction.OnAcceptOfferClick(offer.id)) },
                    onDecline = { onAction(BookingAction.OnDeclineOfferClick(offer.id)) }
                )
            }
        }

        GoZipoButton(
            text = stringResource(Res.string.cancel_search),
            onClick = { onAction(BookingAction.OnCancelRideClick) },
            isLoading = state.isCancelling,
            style = GoZipoButtonStyle.DESTRUCTIVE,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OfferCard(
    offer: RideOffer,
    isAccepting: Boolean,
    acceptEnabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    Surface(
        shape = MaterialTheme.shapes.large,
        color = extended.surfaceHigher,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.padding(spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                ProfileAvatar()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.rider.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = extended.textPrimary
                    )
                    offer.rider.vehicle?.let { vehicle ->
                        Text(
                            text = vehicle.description(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary
                        )
                    }
                    Text(
                        text = listOfNotNull(
                            stringResource(
                                if (offer.kind == OfferKind.COUNTER) Res.string.offer_counter
                                else Res.string.offer_accepts_fare
                            ),
                            offer.pickupDistanceMeters?.let {
                                stringResource(Res.string.offer_distance_away, distanceText(it))
                            }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textTertiary
                    )
                }
                Text(
                    text = stringResource(Res.string.fare_amount, formatAmount(offer.amount)),
                    style = MaterialTheme.typography.titleLarge,
                    color = extended.textPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GoZipoButton(
                    text = stringResource(Res.string.decline),
                    onClick = onDecline,
                    enabled = !isAccepting,
                    style = GoZipoButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
                GoZipoButton(
                    text = stringResource(Res.string.accept),
                    onClick = onAccept,
                    enabled = acceptEnabled,
                    isLoading = isAccepting,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun distanceText(meters: Int): String = if (meters < 1_000) {
    stringResource(Res.string.distance_meters, meters)
} else {
    stringResource(Res.string.distance_kilometers, formatKilometers(meters))
}

/** Time left until [expiresAt], ticking every second; zero once it passed or when there is none. */
@Composable
private fun rememberRemaining(expiresAt: Instant?): Duration {
    var remaining by remember(expiresAt) { mutableStateOf(expiresAt.remaining()) }
    LaunchedEffect(expiresAt) {
        while (expiresAt != null && remaining > Duration.ZERO) {
            delay(1.seconds)
            remaining = expiresAt.remaining()
        }
    }
    return remaining
}

private fun Instant?.remaining(): Duration =
    this?.let { (it - Clock.System.now()).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO

private fun Duration.toCountdownText(): String {
    val seconds = inWholeSeconds
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
