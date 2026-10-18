package com.mobile.payment.data.realtime

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.payment.data.dto.RidePaymentUpdateDto
import com.mobile.payment.data.dto.toRidePaymentUpdate
import com.mobile.payment.domain.PaymentEventSource
import com.mobile.payment.domain.RidePaymentUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Decodes `PAYMENT_UPDATED` from the shared realtime socket (`/ws/rides`). */
class RealtimePaymentEventSource(
    private val realtimeClient: RealtimeClient,
    private val logger: GoZipoLogger
) : PaymentEventSource {

    override val updates: Flow<RidePaymentUpdate> = realtimeClient.messages
        .mapNotNull { message ->
            if (message.type != PAYMENT_UPDATED) return@mapNotNull null
            try {
                PaymentJson.decodeFromString<RidePaymentUpdateDto>(message.payload).toRidePaymentUpdate()
            } catch (_: SerializationException) {
                logger.warn("Couldn't decode realtime message $PAYMENT_UPDATED")
                null
            } catch (_: IllegalArgumentException) {
                logger.warn("Couldn't decode realtime message $PAYMENT_UPDATED")
                null
            }
        }

    private companion object {
        const val PAYMENT_UPDATED = "PAYMENT_UPDATED"
        val PaymentJson = Json { ignoreUnknownKeys = true }
    }
}
