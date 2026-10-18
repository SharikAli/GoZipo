plugins {
    alias(libs.plugins.convention.cmp.feature)
}

// Customer card payments: "Pay with card" on the finished ride, and saved cards (add/remove).
// Payment UI comes from :core:payment:api (Stripe in the customer app).
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)
                implementation(projects.core.designsystem)
                implementation(projects.core.presentation)
                implementation(projects.feature.payment.domain)
                implementation(projects.feature.ride.presentation)

                api(projects.core.payment.api)

                implementation(libs.bundles.koin.common)

                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.material.icons.extended)
                implementation(libs.compose.uiToolingPreview)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }

}
