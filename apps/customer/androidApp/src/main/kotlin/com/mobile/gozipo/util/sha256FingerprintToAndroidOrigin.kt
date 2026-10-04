package com.mobile.gozipo.util

import android.util.Base64

fun sha256FingerprintToAndroidOrigin(
    fingerprint: String
): String {
    val hex = fingerprint.replace(":", "").replace(" ", "")

    require(hex.length == 64) {
        "SHA-256 fingerprint must contain 32 bytes / 64 hex characters"
    }

    val bytes = ByteArray(32)

    for (i in bytes.indices) {
        bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }

    val base64Url = Base64.encodeToString(
        bytes,
        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )

    return "android:apk-key-hash:$base64Url"
}