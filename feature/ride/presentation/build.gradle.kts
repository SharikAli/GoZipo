plugins {
    alias(libs.plugins.convention.cmp.library)
}

// Ride UI shared by the customer (booking) and rider (driving) apps: vehicle types, fares, distances.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.feature.ride.domain)
                implementation(projects.core.domain)

                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.material.icons.extended)
            }
        }
    }

}

compose.resources {
    publicResClass = true
}
