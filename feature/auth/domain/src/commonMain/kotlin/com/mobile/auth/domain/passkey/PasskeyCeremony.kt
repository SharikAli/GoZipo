package com.mobile.auth.domain.passkey

/**
 * A started passkey ceremony. [requestJson] is handed to the platform
 * authenticator; [token] identifies the ceremony when verifying.
 * The backend expires it after 5 minutes and accepts it only once.
 */
data class PasskeyCeremony(
    val token: String,
    val requestJson: String
)
