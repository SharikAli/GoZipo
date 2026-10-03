package com.mobile.auth.data.dto.requests

import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class PasskeyLoginVerifyRequest(
    val token: String,
    val credentialJson: String,
    val role: UserRole
)
