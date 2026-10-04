package com.mobile.auth.data.dto.responses

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyCeremonyDto(
    val token: String,
    val requestJson: String
)
