package com.mobile.booking.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.booking.presentation.booking.BookingAction
import com.mobile.booking.presentation.booking.DestinationPin
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.ride.RouteStopRow
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.confirm_destination
import gozipo.feature.booking.presentation.generated.resources.destination_pin_move_map
import gozipo.feature.booking.presentation.generated.resources.destination_pin_title
import gozipo.feature.booking.presentation.generated.resources.dropped_pin
import gozipo.feature.booking.presentation.generated.resources.pickup_finding_address
import org.jetbrains.compose.resources.stringResource

/** "Choose on map" for the destination: the address under the pin, then confirm. */
@Composable
fun ChooseDestinationOnMapSheet(
    pin: DestinationPin,
    onAction: (BookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    ) {
        Text(
            text = stringResource(Res.string.destination_pin_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.extended.textPrimary
        )
        RouteStopRow(
            icon = Icons.Default.Flag,
            text = when {
                pin.point == null -> stringResource(Res.string.destination_pin_move_map)
                pin.isResolving -> stringResource(Res.string.pickup_finding_address)
                else -> pin.address ?: stringResource(Res.string.dropped_pin)
            },
            isPlaceholder = pin.point == null || pin.isResolving
        )
        GoZipoButton(
            text = stringResource(Res.string.confirm_destination),
            onClick = { onAction(BookingAction.OnConfirmDestinationPinClick) },
            enabled = pin.point != null && !pin.isResolving,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
