package com.mobile.core.domain.realtime

/**
 * One message on the realtime socket, in the backend's `{type, payload}` envelope.
 * [payload] is the message body as a JSON string; feature data layers decode it by [type].
 */
data class RealtimeMessage(
    val type: String,
    val payload: String
)
