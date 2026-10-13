package com.mobile.booking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.BookingState
import com.mobile.booking.presentation.util.formatAmount
import com.mobile.booking.presentation.util.toUi
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.components.ride.DefaultVehicleImage
import com.mobile.core.designsystem.components.ride.FareStepper
import com.mobile.core.designsystem.components.ride.GoZipoDivider
import com.mobile.core.designsystem.components.ride.GoZipoSwitchRow
import com.mobile.core.designsystem.components.ride.VehicleOptionItem
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.ride.domain.FareEstimate
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.auto_accept
import gozipo.feature.booking.presentation.generated.resources.fare_amount
import gozipo.feature.booking.presentation.generated.resources.fare_approximate
import gozipo.feature.booking.presentation.generated.resources.fare_decrease
import gozipo.feature.booking.presentation.generated.resources.fare_increase
import gozipo.feature.booking.presentation.generated.resources.fare_recommended
import gozipo.feature.booking.presentation.generated.resources.fare_your_offer
import gozipo.feature.booking.presentation.generated.resources.fares_failed
import gozipo.feature.booking.presentation.generated.resources.find_offers
import gozipo.feature.booking.presentation.generated.resources.loading_fares
import gozipo.feature.booking.presentation.generated.resources.retry
import gozipo.feature.booking.presentation.generated.resources.trip_minutes
import org.jetbrains.compose.resources.stringResource

/**
 * Vehicle types with the backend's fares. The selected one comes first with the fare stepper,
 * then the main action, so both fit in the collapsed sheet; the other types follow.
 */
@Composable
fun ChooseVehicleSheet(
    state: BookingState,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        when {
            state.isLoadingEstimates -> LoadingRow(text = stringResource(Res.string.loading_fares))

            state.estimatesFailed || state.estimates.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.fares_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.extended.textSecondary
                )
                GoZipoButton(
                    text = stringResource(Res.string.retry),
                    onClick = { onAction(BookingAction.OnRetryEstimatesClick) },
                    style = GoZipoButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                val selected = state.selectedEstimate
                if (selected != null) {
                    VehicleEstimateItem(
                        estimate = selected,
                        selected = true,
                        onClick = {},
                        selectedContent = {
                            val fare = state.offeredFare ?: selected.recommendedFare
                            val range = state.fareRange
                            FareStepper(
                                fareText = stringResource(
                                    Res.string.fare_amount,
                                    formatAmount(fare)
                                ),
                                caption = stringResource(
                                    if (fare == selected.recommendedFare) Res.string.fare_recommended
                                    else Res.string.fare_your_offer
                                ),
                                onDecrease = { onAction(BookingAction.OnDecreaseFareClick) },
                                onIncrease = { onAction(BookingAction.OnIncreaseFareClick) },
                                canDecrease = range != null && fare - state.fareStep >= range.first,
                                canIncrease = range != null && fare + state.fareStep <= range.last,
                                decreaseContentDescription = stringResource(Res.string.fare_decrease),
                                increaseContentDescription = stringResource(Res.string.fare_increase),
                            )
                        }
                    )
                    GoZipoSwitchRow(
                        text = stringResource(
                            Res.string.auto_accept,
                            stringResource(
                                Res.string.fare_amount,
                                formatAmount(state.offeredFare ?: selected.recommendedFare)
                            )
                        ),
                        checked = state.autoAccept,
                        onCheckedChange = { onAction(BookingAction.OnAutoAcceptChange(it)) },
                        icon = Icons.AutoMirrored.Filled.Send
                    )
                    GoZipoButton(
                        text = stringResource(Res.string.find_offers),
                        onClick = { onAction(BookingAction.OnFindOffersClick) },
                        isLoading = state.isRequesting,
                        enabled = state.offeredFare != null && state.pickup != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    GoZipoDivider(modifier = Modifier.padding(vertical = spacing.xs))
                }
                state.estimates
                    .filter { it.vehicleType != state.selectedVehicle }
                    .forEach { estimate ->
                        VehicleEstimateItem(
                            estimate = estimate,
                            selected = false,
                            onClick = { onAction(BookingAction.OnVehicleSelected(estimate.vehicleType)) }
                        )
                    }
            }
        }
    }
}

@Composable
private fun VehicleEstimateItem(
    estimate: FareEstimate,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContent: (@Composable () -> Unit)? = null,
) {
    val ui = estimate.vehicleType.toUi()
    VehicleOptionItem(
        title = stringResource(ui.title),
        capacityText = ui.seats.toString(),
        etaText = stringResource(
            Res.string.trip_minutes,
            estimate.duration.inWholeMinutes.coerceAtLeast(1)
        ),
        description = stringResource(ui.description),
        priceText = stringResource(
            Res.string.fare_approximate,
            formatAmount(estimate.recommendedFare)
        ),
        selected = selected,
        onClick = onClick,
        image = { DefaultVehicleImage(icon = ui.icon) },
        selectedContent = selectedContent?.let { content -> { content() } }
    )
}

@Composable
internal fun LoadingRow(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = GoZipoTheme.spacing.xl)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GoZipoTheme.spacing.md)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.extended.textPrimary)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
        }
    }
}
