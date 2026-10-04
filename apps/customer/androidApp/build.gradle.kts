plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = libs.versions.projectApplicationId.get()

    defaultConfig {
        applicationId = libs.versions.projectApplicationId.get()
    }
}

dependencies {
    implementation(projects.apps.customer.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.koin.compose)

    implementation(libs.core.splashscreen)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}