package com.mobile.ride.data.realtime

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.core.domain.realtime.RealtimeMessage
import com.mobile.core.domain.location.GeoPoint
import com.mobile.ride.data.RideJson
import com.mobile.ride.data.dto.IncomingWebSocketType
import com.mobile.ride.data.dto.OfferDto
import com.mobile.ride.data.dto.RideDto
import com.mobile.ride.data.dto.RideRefDto
import com.mobile.ride.data.dto.RiderLocationDto
import com.mobile.ride.data.mappers.toRide
import com.mobile.ride.data.mappers.toRideOffer
import com.mobile.ride.domain.RideEvent
import com.mobile.ride.domain.RideEventSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException

/** Decodes the ride messages of the shared realtime socket (`/ws/rides`). */
class RealtimeRideEventSource(
    private val realtimeClient: RealtimeClient,
    private val logger: GoZipoLogger
) : RideEventSource {

    override val connectionState: StateFlow<ConnectionState> = realtimeClient.connectionState

    override val events: Flow<RideEvent> = realtimeClient.messages
        .mapNotNull { message ->
            try {
                message.toRideEvent()
            } catch (_: SerializationException) {
                // Log the type only: payloads carry addresses and locations
                logger.warn("Couldn't decode realtime ride message ${message.type}")
                null
            } catch (_: IllegalArgumentException) {
                logger.warn("Couldn't decode realtime ride message ${message.type}")
                null
            }
        }

    /** Null for messages this app doesn't handle (e.g. a rider's RIDE_REQUESTED in the customer app). */
    private fun RealtimeMessage.toRideEvent(): RideEvent? = when (type) {
        IncomingWebSocketType.RIDE_UPDATED.name -> RideEvent.RideUpdated(decode<RideDto>().toRide())
        IncomingWebSocketType.SEARCH_ROUND_ENDED.name -> RideEvent.SearchRoundEnded(decode<RideDto>().toRide())
        IncomingWebSocketType.OFFER_RECEIVED.name -> RideEvent.OfferReceived(decode<OfferDto>().toRideOffer())
        IncomingWebSocketType.OFFER_UPDATED.name -> RideEvent.OfferUpdated(decode<OfferDto>().toRideOffer())
        IncomingWebSocketType.RIDER_LOCATION.name -> decode<RiderLocationDto>().let {
            RideEvent.RiderLocation(rideId = it.rideId, point = GeoPoint(it.latitude, it.longitude))
        }

        IncomingWebSocketType.RIDE_UNAVAILABLE.name -> RideEvent.RideUnavailable(decode<RideRefDto>().rideId)
        else -> null
    }

    private inline fun <reified T> RealtimeMessage.decode(): T =
        RideJson.decodeFromString<T>(payload)
}
