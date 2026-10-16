package com.mobile.auth.data.passkey

import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyError
import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerRequestOptionPreferImmediatelyAvailableCredentials
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationErrorFailed
import platform.AuthenticationServices.ASAuthorizationErrorNotInteractive
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialAssertion
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialDescriptor
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialProvider
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistration
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialAttachment
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialAttestationKindNone
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialUserVerificationPreferenceDiscouraged
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialUserVerificationPreferencePreferred
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
import platform.AuthenticationServices.ASAuthorizationRequest
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSinceDate
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/**
 * Passkeys through AuthenticationServices (iOS 16+; the apps target 18.2). The backend's
 * WebAuthn options JSON is turned into an `ASAuthorizationPlatformPublicKeyCredentialProvider`
 * request, and the result is encoded as the same WebAuthn response JSON that Android's
 * Credential Manager returns, so the backend verifies both alike.
 *
 * The RP ID must be listed under `webcredentials` in the app's Associated Domains, and the
 * domain must serve `/.well-known/apple-app-site-association` naming the app.
 *
 * Only error domains, codes and messages are logged, never the request or credential JSON.
 */
class AuthenticationServicesPasskeyAuthenticator(
    private val logger: GoZipoLogger
) : PasskeyAuthenticator {

    private companion object {
        /** Faster than anyone can dismiss a sheet: no sheet was shown. */
        const val NO_CREDENTIAL_SECONDS = 1.0
    }

    private val json = Json { ignoreUnknownKeys = true }

    override val isSupported: Boolean = true

    /** The controller holds its delegate weakly; this keeps the running ceremony's alive. Main thread only. */
    private var activeDelegate: AuthorizationDelegate? = null

    override suspend fun createPasskey(requestJson: String): Result<String, PasskeyError> {
        val options = parse(requestJson) ?: return Result.Failure(PasskeyError.UNKNOWN)
        val rpId = options.objectOrNull("rp")?.stringOrNull("id")
        val user = options.objectOrNull("user")
        val challenge = options.stringOrNull("challenge")?.decodeBase64Url()
        val userId = user?.stringOrNull("id")?.decodeBase64Url()
        val userName = user?.stringOrNull("name")
        if (rpId == null || challenge == null || userId == null || userName == null) {
            logger.warn("Passkey creation options are incomplete")
            return Result.Failure(PasskeyError.UNKNOWN)
        }

        val request = ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
            .createCredentialRegistrationRequestWithChallenge(
                challenge = challenge.toNSData(),
                name = userName,
                userID = userId.toNSData()
            )
        request.attestationPreference = ASAuthorizationPublicKeyCredentialAttestationKindNone
        options.objectOrNull("authenticatorSelection")?.stringOrNull("userVerification")
            ?.toUserVerificationPreference()
            ?.let { request.userVerificationPreference = it }

        return when (val result = perform(request, preferImmediatelyAvailable = false)) {
            is Result.Failure -> result
            is Result.Success -> {
                val credential = result.data as? ASAuthorizationPlatformPublicKeyCredentialRegistration
                val attestationObject = credential?.rawAttestationObject

                if (credential == null || attestationObject == null) {
                    Result.Failure(PasskeyError.UNKNOWN)
                } else {
                    val crossPlatform =
                        credential.attachment == ASAuthorizationPublicKeyCredentialAttachment.ASAuthorizationPublicKeyCredentialAttachmentCrossPlatform

                    Result.Success(
                        credentialJson(
                            credentialId = credential.credentialID.toByteArray(),
                            crossPlatform = crossPlatform,
                            response = buildJsonObject {
                                put(
                                    "clientDataJSON",
                                    credential.rawClientDataJSON.toByteArray().encodeBase64Url()
                                )
                                put(
                                    "attestationObject",
                                    attestationObject.toByteArray().encodeBase64Url()
                                )
                                put("transports", buildJsonArray {
                                    if (!crossPlatform) add(JsonPrimitive("internal"))
                                    add(JsonPrimitive("hybrid"))
                                })
                            }
                        )
                    )
                }
            }
        }
    }

    override suspend fun getPasskey(requestJson: String): Result<String, PasskeyError> {
        val options = parse(requestJson) ?: return Result.Failure(PasskeyError.UNKNOWN)
        val rpId = options.stringOrNull("rpId")
        val challenge = options.stringOrNull("challenge")?.decodeBase64Url()

        if (rpId == null || challenge == null) {
            logger.warn("Passkey request options are incomplete")
            return Result.Failure(PasskeyError.UNKNOWN)
        }

        val request = ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
            .createCredentialAssertionRequestWithChallenge(challenge.toNSData())

        options.stringOrNull("userVerification")
            ?.toUserVerificationPreference()
            ?.let { request.userVerificationPreference = it }

        val allowed =
            (options["allowCredentials"] as? JsonArray).orEmpty().mapNotNull { descriptor ->
                descriptor.jsonObject.stringOrNull("id")?.decodeBase64Url()?.let {
                    ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID = it.toNSData())
                }
            }
        if (allowed.isNotEmpty()) {
            request.allowedCredentials = allowed
        }

        // Like Android's preferImmediatelyAvailableCredentials: with no passkey on the device,
        // fail fast (NO_CREDENTIAL routes to passkey sign-up) instead of offering only a QR code
        return when (val result = perform(request, preferImmediatelyAvailable = true)) {
            is Result.Failure -> result
            is Result.Success -> {
                val credential = result.data as? ASAuthorizationPlatformPublicKeyCredentialAssertion
                val authenticatorData = credential?.rawAuthenticatorData
                val signature = credential?.signature
                if (credential == null || authenticatorData == null || signature == null) {
                    return Result.Failure(PasskeyError.UNKNOWN)
                }
                Result.Success(
                    credentialJson(
                        credentialId = credential.credentialID.toByteArray(),
                        crossPlatform = credential.attachment == ASAuthorizationPublicKeyCredentialAttachment.ASAuthorizationPublicKeyCredentialAttachmentCrossPlatform,
                        response = buildJsonObject {
                            put(
                                "clientDataJSON",
                                credential.rawClientDataJSON.toByteArray().encodeBase64Url()
                            )
                            put(
                                "authenticatorData",
                                authenticatorData.toByteArray().encodeBase64Url()
                            )
                            put("signature", signature.toByteArray().encodeBase64Url())
                            credential.userID?.let {
                                put(
                                    "userHandle",
                                    it.toByteArray().encodeBase64Url()
                                )
                            }
                        }
                    )
                )
            }
        }
    }

    /** Runs the system sheet on the main thread; the result is the credential, or the mapped error. */
    private suspend fun perform(
        request: ASAuthorizationRequest,
        preferImmediatelyAvailable: Boolean,
    ): Result<Any?, PasskeyError> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val startedAt = NSDate()
            val controller = ASAuthorizationController(authorizationRequests = listOf(request))

            lateinit var delegate: AuthorizationDelegate

            delegate = AuthorizationDelegate { credential, error ->
                if (activeDelegate === delegate) activeDelegate = null
                val result: Result<Any?, PasskeyError> = if (error == null) {
                    Result.Success(credential)
                } else {
                    logger.warn("Passkey ceremony failed: ${error.domain} ${error.code} ${error.localizedDescription}")
                    val elapsed = NSDate().timeIntervalSinceDate(startedAt)
                    Result.Failure(error.toPasskeyError(preferImmediatelyAvailable, elapsed))
                }
                if (continuation.isActive) continuation.resume(result)
            }

            activeDelegate = delegate
            controller.delegate = delegate
            controller.presentationContextProvider = delegate
            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) { controller.cancel() }
            }
            if (preferImmediatelyAvailable) {
                controller.performRequestsWithOptions(
                    ASAuthorizationControllerRequestOptionPreferImmediatelyAvailableCredentials
                )
            } else {
                controller.performRequests()
            }
        }
    }

    private fun credentialJson(
        credentialId: ByteArray,
        crossPlatform: Boolean,
        response: JsonObject,
    ): String {
        val id = credentialId.encodeBase64Url()
        return buildJsonObject {
            put("id", id)
            put("rawId", id)
            put("type", "public-key")
            put("authenticatorAttachment", if (crossPlatform) "cross-platform" else "platform")
            put("response", response)
            put("clientExtensionResults", JsonObject(emptyMap()))
        }.toString()
    }

    private fun parse(requestJson: String): JsonObject? = try {
        json.parseToJsonElement(requestJson).jsonObject
    } catch (_: SerializationException) {
        logger.warn("Passkey options aren't valid JSON")
        null
    } catch (_: IllegalArgumentException) {
        logger.warn("Passkey options aren't a JSON object")
        null
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun String.toUserVerificationPreference(): String? = when (this) {
        "required" -> ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
        "preferred" -> ASAuthorizationPublicKeyCredentialUserVerificationPreferencePreferred
        "discouraged" -> ASAuthorizationPublicKeyCredentialUserVerificationPreferenceDiscouraged
        else -> null
    }

    private fun NSError.toPasskeyError(
        preferImmediatelyAvailable: Boolean,
        elapsedSeconds: Double
    ): PasskeyError {
        if (domain != ASAuthorizationErrorDomain) return PasskeyError.UNKNOWN
        return when (code) {
            // With "prefer immediately available", a device without a passkey for this RP fails
            // straight away as "canceled", before any sheet could be shown and dismissed
            ASAuthorizationErrorCanceled -> if (preferImmediatelyAvailable && elapsedSeconds < NO_CREDENTIAL_SECONDS) {
                PasskeyError.NO_CREDENTIAL
            } else {
                PasskeyError.CANCELLED
            }

            ASAuthorizationErrorNotInteractive -> PasskeyError.NO_CREDENTIAL
            // "Application with identifier … is not associated with domain …": Associated Domains
            // or the domain's apple-app-site-association doesn't match the RP ID
            ASAuthorizationErrorFailed -> if (localizedDescription.contains("associated", ignoreCase = true)) {
                PasskeyError.CONFIGURATION
            } else {
                PasskeyError.UNKNOWN
            }

            else -> PasskeyError.UNKNOWN
        }
    }
}
