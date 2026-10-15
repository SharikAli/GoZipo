package com.mobile.core.map.navigation

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.map.api.NavigationProvider
import org.koin.dsl.module

internal actual val platformNavigationModule = module {
    single<NavigationProvider> {
        val token = BuildKonfig.MAPBOX_ACCESS_TOKEN
        if (token.isBlank()) {
            get<GoZipoLogger>().error("MAPBOX_ACCESS_TOKEN is missing from local.properties; navigation is disabled")
            PlaceholderNavigationProvider("MAPBOX_ACCESS_TOKEN is not set")
        } else {
            MapboxTurnByTurnProvider(token, simulate = BuildKonfig.MAPBOX_SIMULATE_NAVIGATION)
        }
    }
}
