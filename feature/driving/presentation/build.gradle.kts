plugins {
    alias(libs.plugins.convention.cmp.feature)
}

// Rider ride flow: vehicle profile, online/offline, live requests and offers, trip steps, earnings.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.feature.ride.domain)
                implementation(projects.feature.ride.presentation)
                implementation(projects.core.domain)
                implementation(projects.core.designsystem)
                implementation(projects.core.presentation)
                implementation(projects.core.map.api)

                implementation(libs.bundles.koin.common)
                implementation(libs.kotlinx.datetime)

                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.material.icons.extended)
                implementation(libs.compose.uiToolingPreview)
            }
        }
    }

}
