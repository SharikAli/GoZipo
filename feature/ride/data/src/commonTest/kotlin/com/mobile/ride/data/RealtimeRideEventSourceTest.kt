package com.mobile.ride.data

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.core.domain.realtime.RealtimeMessage
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.ride.data.realtime.RealtimeRideEventSource
import com.mobile.ride.domain.OfferKind
import com.mobile.ride.domain.RideEvent
import com.mobile.ride.domain.RideStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

class RealtimeRideEventSourceTest {

    private val warnings = mutableListOf<String>()

    private fun source(vararg messages: RealtimeMessage) = RealtimeRideEventSource(
        realtimeClient = object : RealtimeClient {
            override val connectionState: StateFlow<ConnectionState> =
                MutableStateFlow(ConnectionState.CONNECTED)
            override val messages = flowOf(*messages)
            override suspend fun send(message: RealtimeMessage): EmptyResult<DataError.Connection> =
                Result.Success(Unit)
        },
        logger = object : GoZipoLogger {
            override fun debug(message: String) = Unit
            override fun info(message: String) = Unit
            override fun warn(message: String) {
                warnings += message
            }

            override fun error(message: String, throwable: Throwable?) = Unit
        }
    )

    @Test
    fun decodesRideAndOfferMessages() = runTest {
        val events = source(
            RealtimeMessage("RIDE_UPDATED", RIDE_JSON),
            RealtimeMessage("OFFER_RECEIVED", OFFER_JSON),
            RealtimeMessage(
                "RIDER_LOCATION",
                """{"rideId":"ride-1","latitude":24.9,"longitude":67.1}"""
            )
        ).events.toList()

        val ride = assertIs<RideEvent.RideUpdated>(events[0]).ride
        assertEquals(RideStatus.SEARCHING, ride.status)
        assertEquals(560, ride.offeredFare)
        assertEquals(22.minutes, ride.duration)
        assertEquals("Dr Abdus Salam Rd", ride.pickup.address)

        val offer = assertIs<RideEvent.OfferReceived>(events[1]).offer
        assertEquals(OfferKind.COUNTER, offer.kind)
        assertEquals("APV-809", offer.rider.vehicle?.plateNumber)

        val location = assertIs<RideEvent.RiderLocation>(events[2])
        assertEquals(24.9, location.point.latitude)
    }

    @Test
    fun decodesRideRequests() = runTest {
        val event =
            source(RealtimeMessage("RIDE_REQUESTED", OPEN_RIDE_JSON)).events.toList().single()

        val ride = assertIs<RideEvent.RideRequested>(event).ride
        assertEquals(560, ride.offeredFare)
        assertEquals(840, ride.maximumCounterFare)
        assertEquals(2, ride.searchRound)
        assertEquals(null, ride.myOffer)
    }

    @Test
    fun skipsUnknownTypesAndBrokenPayloadsWithoutLoggingThem() = runTest {
        val events = source(
            RealtimeMessage("ERROR", """{"code":"RIDER_OFFLINE"}"""),
            RealtimeMessage("OFFER_UPDATED", """{"secret":"address"}"""),
            RealtimeMessage("RIDE_UNAVAILABLE", """{"rideId":"ride-1"}""")
        ).events.toList()

        assertEquals(listOf<RideEvent>(RideEvent.RideUnavailable("ride-1")), events)
        assertEquals(1, warnings.size)
        // The payload may hold addresses and locations; only the type is logged
        assertEquals(false, warnings.single().contains("secret"))
    }
}
