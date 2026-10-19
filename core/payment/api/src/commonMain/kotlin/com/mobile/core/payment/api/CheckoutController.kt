package com.mobile.core.payment.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/**
 * One checkout on screen, in the app's [PaymentUiMode]:
 * 1. [prepare] with a session from the backend
 * 2. [PaymentUiMode.FLOW_CONTROLLER]: show [CheckoutState.option] with [changePaymentOption];
 *    [PaymentUiMode.EMBEDDED]: show [Content]
 * 3. [confirm]: the provider asks for the intent (`createIntent`) and reports a [CheckoutResult]
 *
 * For [PaymentUiMode.PAYMENT_SHEET], [confirm] presents the sheet.
 */
@Stable
interface CheckoutController {
    val mode: PaymentUiMode
    val state: StateFlow<CheckoutState>

    /** Loads the customer's saved methods and configures the UI. Safe to call again. */
    suspend fun prepare(session: CheckoutSession): Boolean

    /** Opens the provider's method picker (flow controller only; no-op otherwise). */
    fun changePaymentOption()

    fun confirm()

    /** The inline payment methods (embedded only; draws nothing otherwise). */
    @Composable
    fun Content(modifier: Modifier)
}
