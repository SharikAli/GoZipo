package com.mobile.core.payment.stripe.ios

import platform.UIKit.UIView

/*
 * Bridge to the Stripe iOS SDK (StripePaymentSheet), which is Swift-only and can't be called
 * from Kotlin. Swift implements these protocols (apps/customer/iosApp/iosApp/Stripe/
 * GoZipoStripeBridge.swift) and hands the factory to Kotlin at startup
 * (`doInitKoin(mapboxFactory:stripeFactory:)`). Only plain types cross the bridge.
 * All calls happen on the main thread, in both directions.
 */

/** Creates one native checkout per `rememberCheckout` on screen. */
interface GoZipoStripeFactory {
    /** @param mode a `PaymentUiMode` name: PAYMENT_SHEET, FLOW_CONTROLLER or EMBEDDED */
    fun createCheckout(mode: String, listener: GoZipoStripeListener): GoZipoStripeCheckout
}

interface GoZipoStripeCheckout {
    /** Configures the SDK; [completion] gets null on success, else a message to show. */
    fun prepare(config: GoZipoStripeConfig, completion: (String?) -> Unit)

    /** Flow controller: opens Stripe's method picker. */
    fun presentPaymentOptions()

    /** Presents the sheet (payment sheet) or confirms the selected method; ends in [GoZipoStripeListener.onResult]. */
    fun confirm()

    /** Embedded: the element's view once prepared, else null. */
    fun embeddedView(): UIView?

    fun destroy()
}

/** Calls from Swift back into Kotlin. */
interface GoZipoStripeListener {
    /** Stripe needs the intent: [completion] takes the client secret or an error message. */
    fun createIntent(
        savePaymentMethod: Boolean,
        completion: (clientSecret: String?, errorMessage: String?) -> Unit
    )

    fun onPaymentOptionChanged(label: String?)

    /** @param status "completed", "canceled" or "failed" */
    fun onResult(status: String, errorMessage: String?)

    /** Embedded: the element's height in points changed. */
    fun onEmbeddedHeightChanged(height: Double)
}

/**
 * @param amountMinor 0 for setup (save a card)
 *
 * Apple Pay's merchant ID is not here: Swift reads it from Info.plist (`GoZipoAppleMerchantId`),
 * next to the app's Apple Pay entitlement.
 */
data class GoZipoStripeConfig(
    val publishableKey: String,
    val customerId: String,
    val customerSessionClientSecret: String,
    val merchantDisplayName: String,
    val merchantCountry: String,
    val currency: String,
    val amountMinor: Long,
    val setup: Boolean,
)
