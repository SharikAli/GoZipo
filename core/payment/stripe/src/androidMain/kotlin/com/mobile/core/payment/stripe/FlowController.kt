package com.mobile.core.payment.stripe

import android.content.Context
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.PaymentUiMode
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** FlowController: the app shows the selected method; Stripe's sheet only picks it. */
internal class FlowController(
    context: Context,
    onResult: (CheckoutResult) -> Unit,
) : StripeController(context, onResult) {

    override val mode = PaymentUiMode.FLOW_CONTROLLER
    lateinit var flowController: PaymentSheet.FlowController

    override suspend fun configure(session: CheckoutSession): String? =
        suspendCancellableCoroutine { continuation ->
            flowController.configureWithIntentConfiguration(
                intentConfiguration = session.intentConfiguration(),
                configuration = session.sheetConfiguration()
            ) { success, error ->
                if (success) onOption(flowController.getPaymentOption()?.label)
                if (continuation.isActive) {
                    continuation.resume(
                        if (success) null else error?.localizedMessage
                            ?: "Couldn't load payment methods"
                    )
                }
            }
        }

    override fun changePaymentOption() {
        if (state.value.isReady) flowController.presentPaymentOptions()
    }

    override fun confirm() {
        if (state.value.option == null) {
            changePaymentOption()
            return
        }
        startConfirming() ?: return
        flowController.confirm()
    }
}
