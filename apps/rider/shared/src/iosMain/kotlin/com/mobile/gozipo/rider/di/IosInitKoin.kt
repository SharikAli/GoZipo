package com.mobile.gozipo.rider.di

import com.mobile.core.map.mapbox.ios.GoZipoMapboxFactory
import com.mobile.core.map.mapbox.mapboxIosBridgeModule
import com.mobile.core.map.navigation.ios.GoZipoNavigationFactory
import com.mobile.core.map.navigation.navigationIosBridgeModule

/**
 * iOS entry point: Swift passes its Mapbox bridges, for the map (`GoZipoMapboxBridge.swift`)
 * and turn-by-turn navigation (`GoZipoNavigationBridge.swift`).
 */
fun initKoin(
    mapboxFactory: GoZipoMapboxFactory,
    navigationFactory: GoZipoNavigationFactory,
) {
    initKoin {
        modules(
            mapboxIosBridgeModule(mapboxFactory),
            navigationIosBridgeModule(navigationFactory)
        )
    }
}
