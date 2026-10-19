package com.mobile.core.map.navigation

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.map.api.NavigationProvider
import com.mobile.core.map.navigation.ios.GoZipoNavigationFactory
import com.mobile.core.map.navigation.ios.MapboxIosNavigationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformNavigationModule = module {
    single<NavigationProvider> {
        val token = BuildKonfig.MAPBOX_ACCESS_TOKEN
        val factory = getOrNull<GoZipoNavigationFactory>()
        when {
            token.isBlank() -> {
                get<GoZipoLogger>().error("MAPBOX_ACCESS_TOKEN is missing from local.properties; navigation is disabled")
                PlaceholderNavigationProvider("MAPBOX_ACCESS_TOKEN is not set")
            }

            factory == null -> {
                get<GoZipoLogger>().error("No GoZipoNavigationFactory registered; start Koin with doInitKoin(navigationFactory:)")
                PlaceholderNavigationProvider("navigation bridge not registered")
            }

            else -> MapboxIosNavigationProvider(factory, token, simulate = BuildKonfig.MAPBOX_SIMULATE_NAVIGATION)
        }
    }
}

/** Registers the Swift side of the navigation bridge. The rider app's iOS `initKoin` adds this. */
fun navigationIosBridgeModule(factory: GoZipoNavigationFactory): Module = module {
    single<GoZipoNavigationFactory> { factory }
}
