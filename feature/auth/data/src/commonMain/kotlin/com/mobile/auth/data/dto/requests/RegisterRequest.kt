package com.mobile.auth.data.dto.requests

import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: UserRole
)
