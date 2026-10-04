package com.mobile.auth.data.dto.requests

import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class SignupOptionsRequest(
    val email: String,
    val code: String,
    val role: UserRole
)
