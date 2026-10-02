package com.mobile.auth.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class SignupOptionsRequest(
    val email: String,
    val code: String
)
