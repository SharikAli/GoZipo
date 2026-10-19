package com.mobile.core.data.realtime

import kotlinx.serialization.Serializable

@Serializable
data class RealtimeMessageDto(
    val type: String,
    val payload: String
)
