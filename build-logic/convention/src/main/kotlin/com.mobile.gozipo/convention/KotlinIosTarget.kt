package com.mobile.gozipo.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Locale

internal fun Project.configureIosTargets() {
    extensions.configure<KotlinMultiplatformExtension> {
        listOf(
//            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = appFrameworkName()
                isStatic = true
            }
        }
    }
}

/**
 * `:apps:customer:shared` -> `CustomerShared`, `:apps:rider:shared` -> `RiderShared`.
 */
private fun Project.appFrameworkName(): String {
    val appName = requireNotNull(parent?.takeIf { it != rootProject }) {
        "$path must live inside an app folder such as :apps:customer"
    }.name
    return appName.replaceFirstChar { it.titlecase(Locale.ROOT) } + "Shared"
}
