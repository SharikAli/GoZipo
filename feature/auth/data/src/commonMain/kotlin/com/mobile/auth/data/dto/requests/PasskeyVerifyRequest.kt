package com.mobile.auth.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyVerifyRequest(
    val token: String,
    val credentialJson: String
)
