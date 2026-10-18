package com.mobile.gozipo.di

import com.mobile.core.map.mapbox.ios.GoZipoMapboxFactory
import com.mobile.core.map.mapbox.mapboxIosBridgeModule
import com.mobile.core.payment.stripe.ios.GoZipoStripeFactory
import com.mobile.core.payment.stripe.stripeIosBridgeModule

/**
 * iOS entry point: Swift passes its Mapbox bridge (`GoZipoMapboxBridge.swift`) for the map and
 * its Stripe bridge (`GoZipoStripeBridge.swift`) for card payments.
 */
fun initKoin(mapboxFactory: GoZipoMapboxFactory, stripeFactory: GoZipoStripeFactory) {
    initKoin {
        modules(
            mapboxIosBridgeModule(mapboxFactory),
            stripeIosBridgeModule(stripeFactory)
        )
    }
}
