plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // TODO: apply libs.plugins.google.services once com.mobile.gozipo.rider is
    // added to the Firebase project and its google-services.json is in this folder.
    // Until then FCM is unavailable and the device token is null.
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
