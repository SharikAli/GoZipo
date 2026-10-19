package com.mobile.auth.data.passkey

import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.DomError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyError
import com.mobile.core.data.platform.CurrentActivityProvider
import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.util.Result
import kotlinx.coroutines.CancellationException

/**
 * Passkeys through Android Credential Manager. The system sheet needs an
 * Activity, taken from [activityProvider] when each ceremony starts.
 *
 * Only exception types and messages are logged, never the request or
 * credential JSON.
 */
class CredentialManagerPasskeyAuthenticator(
    private val activityProvider: CurrentActivityProvider,
    private val logger: GoZipoLogger
) : PasskeyAuthenticator {

    // Passkeys need Android 9+ (minSdk is 27)
    override val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    override suspend fun createPasskey(requestJson: String): Result<String, PasskeyError> {
        if (!isSupported) {
            return Result.Failure(PasskeyError.NOT_SUPPORTED)
        }
        val activity = activityProvider.currentActivity
            ?: return Result.Failure(PasskeyError.UNKNOWN)

        return try {
            val response = CredentialManager.create(activity).createCredential(
                context = activity,
                request = CreatePublicKeyCredentialRequest(requestJson = requestJson)
            )
            val registrationJson = (response as? CreatePublicKeyCredentialResponse)?.registrationResponseJson
            if (registrationJson != null) {
                Result.Success(registrationJson)
            } else {
                Result.Failure(PasskeyError.UNKNOWN)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: CreateCredentialException) {
            logger.warn("Passkey creation failed: ${e.type} ${e.errorMessage}")
            Result.Failure(e.toPasskeyError())
        }
    }

    override suspend fun getPasskey(requestJson: String): Result<String, PasskeyError> {
        if (!isSupported) {
            return Result.Failure(PasskeyError.NOT_SUPPORTED)
        }
        val activity = activityProvider.currentActivity
            ?: return Result.Failure(PasskeyError.UNKNOWN)

        return try {
            val response = CredentialManager.create(activity).getCredential(
                context = activity,
                request = GetCredentialRequest(
                    credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson = requestJson)),
                    // Without this, a device with no passkey shows the system "No available
                    // sign-in" sheet instead of failing with NoCredentialException, so the
                    // user never reaches passkey sign-up.
                    preferImmediatelyAvailableCredentials = true
                )
            )
            val credential = response.credential as? PublicKeyCredential
            if (credential != null) {
                Result.Success(credential.authenticationResponseJson)
            } else {
                Result.Failure(PasskeyError.UNKNOWN)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialException) {
            logger.warn("Passkey sign-in failed: ${e.type} ${e.errorMessage}")
            Result.Failure(e.toPasskeyError())
        }
    }

    private fun CreateCredentialException.toPasskeyError(): PasskeyError = when (this) {
        is CreateCredentialCancellationException -> PasskeyError.CANCELLED
        is CreateCredentialNoCreateOptionException,
        is CreateCredentialProviderConfigurationException,
        is CreateCredentialUnsupportedException -> PasskeyError.NOT_SUPPORTED

        is CreatePublicKeyCredentialDomException -> domError.toPasskeyError()
        else -> PasskeyError.UNKNOWN
    }

    private fun GetCredentialException.toPasskeyError(): PasskeyError = when (this) {
        is GetCredentialCancellationException -> PasskeyError.CANCELLED
        is NoCredentialException -> PasskeyError.NO_CREDENTIAL
        is GetCredentialProviderConfigurationException,
        is GetCredentialUnsupportedException -> PasskeyError.NOT_SUPPORTED

        is GetPublicKeyCredentialDomException -> domError.toPasskeyError()
        else -> PasskeyError.UNKNOWN
    }

    private fun DomError.toPasskeyError(): PasskeyError = when (this) {
        // Dismissed or timed out
        is NotAllowedError -> PasskeyError.CANCELLED
        // RP ID doesn't match a domain whose assetlinks.json lists this app
        is SecurityError -> PasskeyError.CONFIGURATION
        else -> PasskeyError.UNKNOWN
    }
}
