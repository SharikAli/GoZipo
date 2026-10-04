package com.mobile.gozipo.convention

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.gradle.api.Project

/**
 * Public HTTPS host of the email App Links, from `APP_LINK_HOST` in local.properties.
 * It must serve `/.well-known/assetlinks.json` (today the backend's Cloudflare tunnel),
 * otherwise Android can't verify the links and opens them in the browser.
 */
fun Project.appLinkHost(): String {
    return gradleLocalProperties(rootDir, rootProject.providers)
        .getProperty("APP_LINK_HOST")
        ?: throw IllegalStateException(
            "Missing APP_LINK_HOST property in local.properties (e.g. your-tunnel.trycloudflare.com)"
        )
}
