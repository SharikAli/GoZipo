rootProject.name = "GoZipo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                // Cronet, pulled in by the Mapbox SDK
                includeGroupAndSubgroups("org.chromium")
            }
        }
        mavenCentral()
        // Mapbox Maps SDK for Android (public repository, no credentials needed)
        maven("https://api.mapbox.com/downloads/v2/releases/maven") {
            mavenContent {
                includeGroupAndSubgroups("com.mapbox")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":apps:common")
include(":apps:customer:androidApp")
include(":apps:customer:shared")
include(":apps:rider:androidApp")
include(":apps:rider:shared")
include(":core:data")
include(":core:presentation")
include(":core:domain")
include(":core:designsystem")
include(":core:map:api")
include(":core:map:mapbox")
include(":core:map:navigation")
include(":feature:auth:domain")
include(":feature:auth:presentation")
include(":feature:auth:data")
include(":feature:ride:domain")
include(":feature:ride:data")
include(":feature:ride:presentation")
include(":feature:driving:presentation")
include(":feature:booking:presentation")
