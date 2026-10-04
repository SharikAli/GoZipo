package com.mobile.auth.data.dto.requests

import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val role: UserRole
)