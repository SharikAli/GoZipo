package com.mobile.core.payment.stripe

import com.mobile.core.payment.api.PaymentUiMode
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val platformStripeModule: Module

/**
 * Binds Stripe as the payment provider (`PaymentProvider`) and the build's [PaymentUiMode]
 * (`STRIPE_PAYMENT_UI`). The customer app includes this in `initKoin`.
 */
val stripePaymentModule = module {
    includes(platformStripeModule)

    single { PaymentUiMode.fromSetting(BuildKonfig.STRIPE_PAYMENT_UI) }
}

internal object StripeConfig {
    /** Cards only; Google Pay and Apple Pay are card wallets, so they come with it. */
    const val PAYMENT_METHOD_CARD = "card"

    fun isLiveKey(publishableKey: String) = publishableKey.startsWith("pk_live_")
}
