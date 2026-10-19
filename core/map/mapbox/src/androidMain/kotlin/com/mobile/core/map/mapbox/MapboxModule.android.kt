package com.mobile.core.map.mapbox

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.map.api.MapProvider
import org.koin.dsl.module

internal actual val platformMapboxModule = module {
    single<MapProvider> {
        val token = BuildKonfig.MAPBOX_ACCESS_TOKEN
        if (token.isBlank()) {
            get<GoZipoLogger>().error("MAPBOX_ACCESS_TOKEN is missing from local.properties; maps are disabled")
            PlaceholderMapProvider("Map unavailable: MAPBOX_ACCESS_TOKEN is not set")
        } else {
            MapboxMapProvider(token)
        }
    }
}
