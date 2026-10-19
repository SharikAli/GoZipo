package com.mobile.core.map.navigation

import org.koin.core.module.Module

internal expect val platformNavigationModule: Module

/**
 * Binds the Mapbox Navigation SDK as the app's `NavigationProvider`. Only the rider app
 * includes this in `initKoin`.
 */
val mapboxNavigationModule: Module = platformNavigationModule
