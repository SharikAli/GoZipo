package com.mobile.core.payment.stripe.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mobile.core.payment.api.CheckoutController
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.payment.api.PaymentProvider
import com.mobile.core.payment.api.PaymentUiMode

/** Stripe's iOS payment UIs through the Swift bridge ([GoZipoStripeFactory]). */
internal class StripeIosPaymentProvider(
    private val factory: GoZipoStripeFactory
) : PaymentProvider {

    @Composable
    override fun rememberCheckoutController(
        mode: PaymentUiMode,
        createIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController {
        val scope = rememberCoroutineScope()
        val controller = remember {
            IosCheckoutController(
                mode = mode,
                scope = scope,
                requestIntent = createIntent,
                deliverResult = onResult
            ).also { controller ->
                controller.checkout = factory.createCheckout(mode.name, controller.listener)
            }
        }
        DisposableEffect(controller) {
            onDispose { controller.checkout.destroy() }
        }
        return controller
    }
}
