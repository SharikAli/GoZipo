plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.feature.auth.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.compose)
                implementation(libs.androidx.credentials)
                // Credential Manager support on Android 13 and lower
                implementation(libs.androidx.credentials.play.services.auth)
            }
        }

        iosMain {
            dependencies {

            }
        }

    }

}