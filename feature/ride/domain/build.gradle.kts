plugins {
    alias(libs.plugins.convention.kmp.library)
}

// Ride models and contracts shared by both apps (customer booking, rider driving).
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
