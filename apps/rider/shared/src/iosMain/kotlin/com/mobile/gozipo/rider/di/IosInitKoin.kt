package com.mobile.gozipo.rider.di

import com.mobile.core.map.mapbox.ios.GoZipoMapboxFactory
import com.mobile.core.map.mapbox.mapboxIosBridgeModule

/** iOS entry point: Swift passes its Mapbox bridge (`GoZipoMapboxBridge.swift`) for the map. */
fun initKoin(mapboxFactory: GoZipoMapboxFactory) {
    initKoin {
        modules(mapboxIosBridgeModule(mapboxFactory))
    }
}
