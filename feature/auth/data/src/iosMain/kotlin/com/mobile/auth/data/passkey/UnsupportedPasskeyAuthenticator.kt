package com.mobile.auth.data.passkey

import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyError
import com.mobile.core.domain.util.Result

/** Passkeys are Android-only for now; the UI hides passkey options when unsupported. */
class UnsupportedPasskeyAuthenticator : PasskeyAuthenticator {
    override val isSupported: Boolean = false

    override suspend fun createPasskey(requestJson: String): Result<String, PasskeyError> =
        Result.Failure(PasskeyError.NOT_SUPPORTED)

    override suspend fun getPasskey(requestJson: String): Result<String, PasskeyError> =
        Result.Failure(PasskeyError.NOT_SUPPORTED)
}
