plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

// Mapbox Navigation SDK implementation of the turn-by-turn contract (:core:map:api
// NavigationProvider). Separate from :core:map:mapbox so only the rider app ships it.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.domain)
                implementation(projects.core.map.api)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.material.icons.extended)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.mapbox.maps)
                implementation(libs.mapbox.navigation.core)
                implementation(libs.mapbox.navigation.ui.components)
                // The SDK views extend these but only declare them for runtime
                implementation(libs.androidx.constraintlayout)
                implementation(libs.google.material)
            }
        }
    }

}
