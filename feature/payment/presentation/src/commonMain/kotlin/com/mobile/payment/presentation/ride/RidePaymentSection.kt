package com.mobile.payment.presentation.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.buttons.GoZipoButtonStyle
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.PaymentOption
import com.mobile.core.payment.api.PaymentUiMode
import com.mobile.core.payment.api.rememberCheckout
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.payment.domain.RidePaymentStage
import com.mobile.payment.presentation.components.CheckoutPanel
import com.mobile.ride.presentation.util.fareText
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.pay_amount
import gozipo.feature.payment.presentation.generated.resources.pay_in_cash_instead
import gozipo.feature.payment.presentation.generated.resources.pay_with_card
import gozipo.feature.payment.presentation.generated.resources.payment_check_again
import gozipo.feature.payment.presentation.generated.resources.payment_paid_body
import gozipo.feature.payment.presentation.generated.resources.payment_paid_title
import gozipo.feature.payment.presentation.generated.resources.payment_processing
import gozipo.feature.payment.presentation.generated.resources.payment_refunded
import gozipo.feature.payment.presentation.generated.resources.payment_verifying
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Card payment for a finished ride, inside its sheet. Cash stays the default: the customer
 * can ignore this and tap Done as before.
 *
 * @param fare whole PKR
 */
@Composable
fun RidePaymentRoot(
    rideId: String,
    fare: Int,
    modifier: Modifier = Modifier,
    viewModel: RidePaymentViewModel = koinViewModel(key = "ride-payment-$rideId") {
        parametersOf(rideId, fare)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val checkout = rememberCheckout(
        createIntent = viewModel::createIntent,
        onResult = { viewModel.onAction(RidePaymentAction.OnCheckoutResult(it)) }
    )
    val checkoutState by checkout.state.collectAsState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RidePaymentEvent.StartCheckout -> scope.launch {
                val ready = checkout.prepare(event.session)
                viewModel.onAction(
                    RidePaymentAction.OnCheckoutPrepared(ready, checkout.state.value.errorMessage)
                )
                // The sheet has no app UI of its own: present it right away
                if (ready && !checkout.mode.hasOwnConfirmButton) checkout.confirm()
            }
        }
    }

    RidePaymentSection(
        state = state,
        mode = checkout.mode,
        checkoutState = checkoutState,
        onAction = viewModel::onAction,
        onConfirmClick = checkout::confirm,
        onChangeMethodClick = checkout::changePaymentOption,
        embeddedContent = { checkout.Content(it) },
        modifier = modifier
    )
}

@Composable
fun RidePaymentSection(
    state: RidePaymentState,
    mode: PaymentUiMode,
    checkoutState: CheckoutState,
    onAction: (RidePaymentAction) -> Unit,
    onConfirmClick: () -> Unit,
    onChangeMethodClick: () -> Unit,
    embeddedContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoZipoTheme.spacing
    val extended = MaterialTheme.colorScheme.extended
    val amountText = fareText(state.amount)

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal)
    ) {
        when (state.stage) {
            RidePaymentStage.IDLE, RidePaymentStage.PREPARING -> {
                if (state.payable || state.stage == RidePaymentStage.PREPARING) {
                    GoZipoButton(
                        text = stringResource(Res.string.pay_with_card),
                        onClick = { onAction(RidePaymentAction.OnPayWithCardClick) },
                        isLoading = state.stage == RidePaymentStage.PREPARING,
                        style = GoZipoButtonStyle.OUTLINED,
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.refunded) {
                    Text(
                        text = stringResource(Res.string.payment_refunded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                }
            }

            RidePaymentStage.CHECKOUT -> {
                CheckoutPanel(
                    mode = mode,
                    checkoutState = checkoutState,
                    confirmText = stringResource(Res.string.pay_amount, amountText),
                    onConfirmClick = onConfirmClick,
                    onChangeMethodClick = onChangeMethodClick,
                    embeddedContent = embeddedContent
                )
                if (!mode.hasOwnConfirmButton) {
                    // The sheet is open (or was closed): offer it again
                    GoZipoButton(
                        text = stringResource(Res.string.pay_with_card),
                        onClick = onConfirmClick,
                        isLoading = checkoutState.isConfirming,
                        style = GoZipoButtonStyle.OUTLINED,
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                GoZipoButton(
                    text = stringResource(Res.string.pay_in_cash_instead),
                    onClick = { onAction(RidePaymentAction.OnPayInCashClick) },
                    style = GoZipoButtonStyle.TEXT,
                    enabled = !checkoutState.isConfirming,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            RidePaymentStage.VERIFYING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    if (!state.stillProcessing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(spacing.xxs))
                    }
                    Text(
                        text = stringResource(
                            if (state.stillProcessing) Res.string.payment_processing else Res.string.payment_verifying
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = extended.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.stillProcessing) {
                    GoZipoButton(
                        text = stringResource(Res.string.payment_check_again),
                        onClick = { onAction(RidePaymentAction.OnCheckAgainClick) },
                        style = GoZipoButtonStyle.TEXT,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            RidePaymentStage.PAID -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = extended.success)
                Column {
                    Text(
                        text = stringResource(Res.string.payment_paid_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = extended.textPrimary
                    )
                    Text(
                        text = stringResource(Res.string.payment_paid_body, amountText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                }
            }
        }

        state.errorText?.let { error ->
            Text(
                text = error.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview
@Composable
private fun RidePaymentSectionPreview() {
    GoZipoTheme(darkTheme = true) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                RidePaymentStage.entries.forEach { stage ->
                    RidePaymentSection(
                        state = RidePaymentState(rideId = "1", amount = 593, stage = stage, payable = true),
                        mode = PaymentUiMode.FLOW_CONTROLLER,
                        checkoutState = CheckoutState(isReady = true, option = PaymentOption("•••• 4242")),
                        onAction = {},
                        onConfirmClick = {},
                        onChangeMethodClick = {},
                        embeddedContent = {}
                    )
                }
            }
        }
    }
}
