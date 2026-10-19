package com.mobile.core.payment.api

import androidx.compose.runtime.Composable

/**
 * A payment SDK behind [rememberCheckout]. Today `StripePaymentProvider` (:core:payment:stripe);
 * another provider would implement this and be bound in Koin instead.
 *
 * Implementations call their SDK's remember functions unconditionally (SDKs register activity
 * result handlers during composition), so [mode] must not change for a given call site.
 */
interface PaymentProvider {

    /**
     * @param createIntent asks the backend for the PaymentIntent/SetupIntent at confirmation;
     * `savePaymentMethod` is the customer's "save this card" choice
     */
    @Composable
    fun rememberCheckoutController(
        mode: PaymentUiMode,
        createIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController
}
