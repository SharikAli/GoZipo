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
            }
        }
        mavenCentral()
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
include(":feature:auth:domain")
include(":feature:auth:presentation")
include(":feature:auth:data")
