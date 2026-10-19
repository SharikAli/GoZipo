package com.mobile.ride.data

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.ride.data.remote.KtorRideService
import com.mobile.ride.domain.VehicleType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class KtorRideServiceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun service(status: HttpStatusCode, body: String) = KtorRideService(
        HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
    )

    @Test
    fun noActiveRideIsNull() = runTest {
        val result = service(HttpStatusCode.NoContent, "").getActiveRide()

        assertNull((result as Result.Success).data)
        assertEquals("/api/rides/active", requests.single().url.encodedPath)
    }

    @Test
    fun activeRideIsDecoded() = runTest {
        val result = service(HttpStatusCode.OK, RIDE_JSON).getActiveRide()

        assertEquals(VehicleType.MINI, (result as Result.Success).data?.vehicleType)
    }

    @Test
    fun conflictMapsToConflict() = runTest {
        val result =
            service(HttpStatusCode.Conflict, """{"code":"ACTIVE_RIDE_EXISTS"}""").getActiveRide()

        assertEquals(DataError.Remote.CONFLICT, (result as Result.Failure).error)
    }

    @Test
    fun estimatesAreMapped() = runTest {
        val body =
            """[{"vehicleType":"MOTO","recommendedFare":240,"minimumFare":170,"maximumFare":720,
            |"fareStep":10,"distanceMeters":9800,"durationSeconds":1080}]""".trimMargin()

        val result = service(HttpStatusCode.OK, body).estimateFares(
            GeoPoint(24.94, 67.11),
            GeoPoint(24.93, 67.07)
        )

        val estimate = (result as Result.Success).data.single()
        assertEquals(VehicleType.MOTO, estimate.vehicleType)
        assertEquals(18.minutes, estimate.duration)
        assertEquals("/api/rides/estimate", requests.single().url.encodedPath)
    }

    @Test
    fun takingTheFareSendsNoAmount() = runTest {
        val result = service(HttpStatusCode.OK, OFFER_JSON).submitOffer("ride-1")

        assertEquals("offer-1", (result as Result.Success).data.id)
        val request = requests.single()
        assertEquals("/api/rides/ride-1/offers", request.url.encodedPath)
        assertEquals("{}", request.body.toByteArray().decodeToString())
    }

    @Test
    fun counterOfferSendsTheAmount() = runTest {
        service(HttpStatusCode.OK, OFFER_JSON).submitOffer("ride-1", amount = 600)

        assertEquals("""{"amount":600}""", requests.single().body.toByteArray().decodeToString())
    }

    @Test
    fun openRidesAreMapped() = runTest {
        val result = service(HttpStatusCode.OK, "[$OPEN_RIDE_JSON]").getOpenRides()

        assertEquals(850, (result as Result.Success).data.single().pickupDistanceMeters)
        assertEquals(HttpMethod.Get, requests.single().method)
    }

    @Test
    fun tripStepsPostToTheirRoute() = runTest {
        val service = service(HttpStatusCode.OK, RIDE_JSON)
        service.markArrived("ride-1")
        service.startTrip("ride-1")
        service.completeTrip("ride-1")

        assertEquals(
            listOf(
                "/api/rides/ride-1/arrived",
                "/api/rides/ride-1/start",
                "/api/rides/ride-1/complete"
            ),
            requests.map { it.url.encodedPath }
        )
    }
}
