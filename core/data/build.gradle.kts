plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)
                implementation(libs.bundles.ktor.common)
                implementation(libs.touchlab.kermit)
                implementation(libs.koin.core)

                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                implementation(libs.androidx.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.koin.compose)
                implementation(libs.androidx.lifecycle.process)
                implementation(libs.play.services.location)

                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

    }

}