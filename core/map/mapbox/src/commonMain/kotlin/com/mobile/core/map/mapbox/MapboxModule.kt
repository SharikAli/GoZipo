package com.mobile.core.map.mapbox

import com.mobile.core.domain.place.PlaceSearchService
import com.mobile.core.domain.route.RouteService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect val platformMapboxModule: Module

private val MAPBOX_HTTP_CLIENT = named("mapboxHttpClient")

/**
 * Binds Mapbox as the map provider (`MapProvider`), place search and routing. Apps include
 * this in `initKoin`; another provider's module would replace it there and nowhere else.
 */
val mapboxModule = module {
    includes(platformMapboxModule)

    single(MAPBOX_HTTP_CLIENT) { createMapboxHttpClient(get()) }
    single<PlaceSearchService> {
        MapboxPlaceSearchService(get(MAPBOX_HTTP_CLIENT), BuildKonfig.MAPBOX_ACCESS_TOKEN)
    }
    single<RouteService> {
        MapboxRouteService(get(MAPBOX_HTTP_CLIENT), BuildKonfig.MAPBOX_ACCESS_TOKEN)
    }
}
