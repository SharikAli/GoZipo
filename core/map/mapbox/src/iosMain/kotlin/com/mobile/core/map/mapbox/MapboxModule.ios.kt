package com.mobile.core.map.mapbox

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.map.api.MapProvider
import com.mobile.core.map.mapbox.ios.GoZipoMapboxFactory
import com.mobile.core.map.mapbox.ios.MapboxIosMapProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformMapboxModule = module {
    single<MapProvider> {
        val token = BuildKonfig.MAPBOX_ACCESS_TOKEN
        val factory = getOrNull<GoZipoMapboxFactory>()
        when {
            token.isBlank() -> {
                get<GoZipoLogger>().error("MAPBOX_ACCESS_TOKEN is missing from local.properties; maps are disabled")
                PlaceholderMapProvider("Map unavailable: MAPBOX_ACCESS_TOKEN is not set")
            }

            factory == null -> {
                get<GoZipoLogger>().error("No GoZipoMapboxFactory registered; start Koin with doInitKoin(mapboxFactory:)")
                PlaceholderMapProvider("Map unavailable: Mapbox bridge not registered")
            }

            else -> MapboxIosMapProvider(factory, token)
        }
    }
}

/** Registers the Swift side of the Mapbox bridge. Each iOS app's `initKoin` adds this. */
fun mapboxIosBridgeModule(factory: GoZipoMapboxFactory): Module = module {
    single<GoZipoMapboxFactory> { factory }
}
