package com.mobile.core.data.network

import com.mobile.core.data.BuildKonfig

object UrlConstants {
    // Android Emulator
    const val ANDROID_EMULATOR_BASE_URL_HTTP = "http://10.0.2.2:8080/api"
    const val ANDROID_EMULATOR_BASE_URL_WS = "ws://10.0.2.2:8080/ws"

    // IOS simulator
    const val BASE_URL_HTTP = "http://localhost:8080/api"
    const val BASE_URL_WS = "ws://localhost:8080/ws"

    // Public HTTPS host of the email App Links (APP_LINK_HOST in local.properties)
    val APP_LINK_HOST: String = BuildKonfig.APP_LINK_HOST
}
