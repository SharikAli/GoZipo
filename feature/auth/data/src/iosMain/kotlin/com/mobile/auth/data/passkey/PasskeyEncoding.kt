package com.mobile.auth.data.passkey

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.io.encoding.Base64

/** WebAuthn JSON carries binary fields as unpadded base64url. */
private val Base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

internal fun String.decodeBase64Url(): ByteArray? = try {
    Base64Url.decode(this)
} catch (_: IllegalArgumentException) {
    null
}

internal fun ByteArray.encodeBase64Url(): String =
    Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(this)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
