package com.mobile.auth.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class LoginOptionsRequest(
    val email: String? = null
)
