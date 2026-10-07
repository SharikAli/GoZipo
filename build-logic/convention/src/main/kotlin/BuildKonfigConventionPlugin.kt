import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mobile.gozipo.convention.appLinkHost
import com.mobile.gozipo.convention.pathToPackageName
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class BuildKonfigConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.codingfeline.buildkonfig")
            }

            extensions.configure<BuildKonfigExtension> {
                packageName.set(target.pathToPackageName())
                defaultConfigs {
                    val localProperties = gradleLocalProperties(rootDir, rootProject.providers)
                    val apiKey = localProperties
                        .getProperty("API_KEY")
                        ?: throw IllegalStateException(
                            "Missing API_KEY property in local.properties"
                        )
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", apiKey)

                    // Host of the email App Links (verify / reset password); also set in each app's manifest
                    buildConfigField(FieldSpec.Type.STRING, "APP_LINK_HOST", appLinkHost())

                    // Mapbox public token (pk.…) for maps, search and directions. Optional so the
                    // project builds without it; maps stay blank and Mapbox calls fail until it's set.
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "MAPBOX_ACCESS_TOKEN",
                        localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
                    )

                    // HTTP logging; sensitive values are masked unless HTTP_LOG_SENSITIVE_DATA=true (development only)
                    buildConfigField(
                        FieldSpec.Type.BOOLEAN,
                        "HTTP_LOGGING_ENABLED",
                        localProperties.getProperty("HTTP_LOGGING_ENABLED") ?: "true"
                    )
                    buildConfigField(
                        FieldSpec.Type.BOOLEAN,
                        "HTTP_LOG_SENSITIVE_DATA",
                        localProperties.getProperty("HTTP_LOG_SENSITIVE_DATA") ?: "false"
                    )
                }
            }
        }
    }
}