package com.mobile.core.data.network

actual fun platformBaseUrl(): String {
    return UrlConstants.ANDROID_EMULATOR_BASE_URL_HTTP
}

actual fun platformWebSocketBaseUrl(): String {
    return UrlConstants.ANDROID_EMULATOR_BASE_URL_WS
}