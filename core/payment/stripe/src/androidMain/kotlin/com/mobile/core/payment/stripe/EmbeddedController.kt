package com.mobile.core.payment.stripe

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.PaymentUiMode
import com.stripe.android.paymentelement.EmbeddedPaymentElement

/** Embedded Payment Element: methods drawn in [Content]; the app's button confirms. */
internal class EmbeddedController(
    context: Context,
    onResult: (CheckoutResult) -> Unit,
) : StripeController(context, onResult) {

    override val mode = PaymentUiMode.EMBEDDED
    lateinit var element: EmbeddedPaymentElement

    override suspend fun configure(session: CheckoutSession): String? {
        val configuration =
            EmbeddedPaymentElement.Configuration.Builder(session.merchantDisplayName)
                .customer(session.customerConfiguration())
                .googlePay(session.googlePayConfiguration())
                .cardBrandAcceptance(cardBrandAcceptance)
                .allowsDelayedPaymentMethods(false)
                .build()
        return when (val result = element.configure(session.intentConfiguration(), configuration)) {
            is EmbeddedPaymentElement.ConfigureResult.Succeeded -> null
            is EmbeddedPaymentElement.ConfigureResult.Failed -> result.error.localizedMessage
                ?: "Couldn't load payment methods"
        }
    }

    override fun confirm() {
        if (state.value.option == null) return
        startConfirming() ?: return
        element.confirm()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier) {
            element.Content()
        }
    }
}