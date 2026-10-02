package com.mobile.auth.domain.passkey

import com.mobile.core.domain.util.Result

/**
 * Platform authenticator (Credential Manager on Android). Takes the
 * WebAuthn options JSON from the backend and returns the credential JSON
 * to send back for verification.
 */
interface PasskeyAuthenticator {
    val isSupported: Boolean

    suspend fun createPasskey(requestJson: String): Result<String, PasskeyError>

    suspend fun getPasskey(requestJson: String): Result<String, PasskeyError>
}
