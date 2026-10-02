package com.mobile.auth.presentation.passkey

import com.mobile.auth.domain.passkey.PasskeyError
import com.mobile.core.presentation.util.UiText
import gozipo.feature.auth.presentation.generated.resources.Res
import gozipo.feature.auth.presentation.generated.resources.error_passkey_configuration
import gozipo.feature.auth.presentation.generated.resources.error_passkey_not_supported
import gozipo.feature.auth.presentation.generated.resources.error_passkey_unknown

/** Null for errors that need no message (canceled) or are handled by navigation (no credential). */
fun PasskeyError.toUiTextOrNull(): UiText? = when (this) {
    PasskeyError.CANCELLED,
    PasskeyError.NO_CREDENTIAL -> null
    PasskeyError.NOT_SUPPORTED -> UiText.Resource(Res.string.error_passkey_not_supported)
    PasskeyError.CONFIGURATION -> UiText.Resource(Res.string.error_passkey_configuration)
    PasskeyError.UNKNOWN -> UiText.Resource(Res.string.error_passkey_unknown)
}
