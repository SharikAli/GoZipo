plugins {
    alias(libs.plugins.convention.cmp.library)
}

// Map UI contract. Features draw maps only through GoZipoMap and these types; the
// provider (today :core:map:mapbox) is bound in each app's Koin setup.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.domain)

                implementation(libs.koin.compose)
            }
        }
    }

}
