package com.mobile.auth.data.dto.responses

import com.mobile.core.data.dto.UserSerializable
import kotlinx.serialization.Serializable

@Serializable
data class PasskeyAuthResponseDto(
    val user: UserSerializable,
    val accessToken: String,
    val refreshToken: String,
    val providerName: String? = null
)
