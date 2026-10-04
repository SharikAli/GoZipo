package com.mobile.core.data.dto

import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class UserSerializable(
    val id: String,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean,
    // Default for sessions stored before roles existed
    val role: UserRole = UserRole.CUSTOMER,
    val profilePictureUrl: String? = null
)