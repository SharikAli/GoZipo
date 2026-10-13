package com.mobile.booking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.BookingState
import com.mobile.booking.presentation.booking.SearchTarget
import com.mobile.core.designsystem.components.ride.RouteStopsCard
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.domain.location.LocationError
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.dropoff_where_to
import gozipo.feature.booking.presentation.generated.resources.location_permission_denied
import gozipo.feature.booking.presentation.generated.resources.location_services_disabled
import gozipo.feature.booking.presentation.generated.resources.location_unavailable
import gozipo.feature.booking.presentation.generated.resources.pickup_finding_address
import gozipo.feature.booking.presentation.generated.resources.pickup_move_map
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Pickup from the pin (or search), then "Where to?". */
@Composable
fun ChooseStopsSheet(
    state: BookingState,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        RouteStopsCard(
            pickupText = when {
                state.isResolvingPickup -> stringResource(Res.string.pickup_finding_address)
                else -> state.pickupAddress ?: stringResource(Res.string.pickup_move_map)
            },
            pickupPlaceholder = state.pickupAddress == null,
            destinationText = stringResource(Res.string.dropoff_where_to),
            destinationPlaceholder = true,
            onPickupClick = { onAction(BookingAction.OnOpenSearch(SearchTarget.PICKUP)) },
            onDestinationClick = { onAction(BookingAction.OnOpenSearch(SearchTarget.DROPOFF)) },
            elevated = false
        )
        state.locationError?.toMessage()?.let { message ->
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
        }
    }
}

private fun LocationError.toMessage(): StringResource? = when (this) {
    LocationError.PERMISSION_DENIED -> Res.string.location_permission_denied
    LocationError.SERVICES_DISABLED -> Res.string.location_services_disabled
    LocationError.UNAVAILABLE -> Res.string.location_unavailable
    LocationError.UNSUPPORTED -> null
}
