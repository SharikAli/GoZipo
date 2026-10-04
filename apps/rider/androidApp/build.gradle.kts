plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = libs.versions.projectRiderApplicationId.get()

    defaultConfig {
        applicationId = libs.versions.projectRiderApplicationId.get()
    }
}

dependencies {
    implementation(projects.apps.rider.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.koin.compose)

    implementation(libs.core.splashscreen)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
