plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

// Mapbox implementation of the map contract (:core:map:api) and of the domain's
// PlaceSearchService and RouteService. Only the app modules depend on this.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.core.map.api)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.mapbox.maps)
                implementation(libs.mapbox.maps.compose)
            }
        }
    }

}
