package com.mobile.auth.domain.passkey

import com.mobile.core.domain.util.Error

enum class PasskeyError : Error {
    /** The user dismissed the system sheet. Not worth an error message. */
    CANCELLED,

    /** The device has no passkey for this app (login only). */
    NO_CREDENTIAL,

    /** No screen lock / provider, or the OS version is too old. */
    NOT_SUPPORTED,

    /** Domain association (assetlinks.json / RP ID) doesn't match the app. */
    CONFIGURATION,

    UNKNOWN
}