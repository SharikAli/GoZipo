package com.mobile.auth.domain.passkey

import com.mobile.core.domain.auth.AuthInfo
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result

/**
 * Backend side of passkey sign-in. Each ceremony is two calls: options
 * (returns a [PasskeyCeremony] for [PasskeyAuthenticator]) and verify
 * (sends the authenticator's response back with the ceremony token).
 *
 * Sign-up for a device without a passkey: [sendSignupCode] ->
 * [signupOptions] -> [signupVerify]. The emailed code proves ownership, so
 * the account is created verified (or the passkey is linked to an existing one).
 */
interface PasskeyService {
    suspend fun sendSignupCode(email: String): EmptyResult<DataError.Remote>

    suspend fun signupOptions(
        email: String,
        code: String
    ): Result<PasskeyCeremony, DataError.Remote>

    suspend fun signupVerify(
        token: String,
        credentialJson: String
    ): Result<AuthInfo, DataError.Remote>

    /** A null [email] starts a username less login: the system picker lists every passkey for the app. */
    suspend fun loginOptions(email: String? = null): Result<PasskeyCeremony, DataError.Remote>

    suspend fun loginVerify(
        token: String,
        credentialJson: String
    ): Result<AuthInfo, DataError.Remote>
}
