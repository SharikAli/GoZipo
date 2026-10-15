plugins {
    alias(libs.plugins.convention.cmp.feature)
}

// App shell shared by GoZipo and GoZipo Rider: session handling, push token
// registration and deep link plumbing. Each app's :shared module adds its own
// App, navigation and Koin setup on top.
kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.domain)

                implementation(projects.feature.auth.domain)

                implementation(libs.bundles.koin.common)
            }
        }
    }

}
