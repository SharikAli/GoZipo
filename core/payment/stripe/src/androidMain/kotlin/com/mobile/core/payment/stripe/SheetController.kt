package com.mobile.core.payment.stripe

import android.content.Context
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.PaymentUiMode
import com.stripe.android.paymentsheet.PaymentSheet

/** PaymentSheet: [confirm] presents the whole sheet. */
internal class SheetController(
    context: Context,
    onResult: (CheckoutResult) -> Unit,
) : StripeController(context, onResult) {

    override val mode = PaymentUiMode.PAYMENT_SHEET
    lateinit var sheet: PaymentSheet

    // Nothing to load up front: the sheet loads when presented
    override suspend fun configure(session: CheckoutSession): String? = null

    override fun confirm() {
        val session = startConfirming() ?: return
        sheet.presentWithIntentConfiguration(
            intentConfiguration = session.intentConfiguration(),
            configuration = session.sheetConfiguration()
        )
    }
}
