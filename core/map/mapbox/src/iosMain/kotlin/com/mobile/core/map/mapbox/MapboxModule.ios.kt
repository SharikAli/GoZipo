package com.mobile.core.map.mapbox

import com.mobile.core.map.api.MapProvider
import org.koin.dsl.module

// The Mapbox iOS SDK needs a Swift bridge and the secret download token (deferred, see
// Phase 5 in document.md). Search and routing above already work on iOS.
internal actual val platformMapboxModule = module {
    single<MapProvider> { PlaceholderMapProvider("Map coming soon on iOS") }
}
