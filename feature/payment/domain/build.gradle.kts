plugins {
    alias(libs.plugins.convention.kmp.library)
}

// Customer card payments: ride payment status, checkout sessions, saved cards.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)

                implementation(projects.core.domain)
            }
        }
    }

}
