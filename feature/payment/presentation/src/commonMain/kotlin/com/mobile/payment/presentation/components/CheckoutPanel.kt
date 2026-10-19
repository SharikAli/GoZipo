package com.mobile.payment.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.ride.PaymentMethodRow
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.PaymentUiMode
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.payment_method_change
import gozipo.feature.payment.presentation.generated.resources.payment_method_choose
import gozipo.feature.payment.presentation.generated.resources.payment_method_label
import org.jetbrains.compose.resources.stringResource

/**
 * The app-drawn part of a prepared checkout. The sheet ([PaymentUiMode.PAYMENT_SHEET]) draws
 * everything itself, so this shows nothing for it; the flow controller gets the selected
 * method with "Change", the embedded element its inline methods, and both a confirm button.
 *
 * @param embeddedContent the controller's `Content`
 */
@Composable
fun CheckoutPanel(
    mode: PaymentUiMode,
    checkoutState: CheckoutState,
    confirmText: String,
    onConfirmClick: () -> Unit,
    onChangeMethodClick: () -> Unit,
    embeddedContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!mode.hasOwnConfirmButton) return
    val spacing = GoZipoTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier.fillMaxWidth()
    ) {
        when (mode) {
            PaymentUiMode.FLOW_CONTROLLER -> PaymentMethodRow(
                label = stringResource(Res.string.payment_method_label),
                amountText = checkoutState.option?.label ?: stringResource(Res.string.payment_method_choose),
                methodText = stringResource(Res.string.payment_method_change),
                icon = Icons.Default.CreditCard,
                onClick = onChangeMethodClick
            )

            PaymentUiMode.EMBEDDED -> embeddedContent(Modifier.fillMaxWidth())
            PaymentUiMode.PAYMENT_SHEET -> Unit
        }
        GoZipoButton(
            text = confirmText,
            onClick = onConfirmClick,
            isLoading = checkoutState.isConfirming,
            // Flow controller: with no method yet, confirming opens the picker
            enabled = checkoutState.isReady &&
                (mode == PaymentUiMode.FLOW_CONTROLLER || checkoutState.option != null),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
