package com.mobile.ride.data

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.realtime.RealtimeClient
import com.mobile.core.domain.realtime.RealtimeMessage
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.ride.data.remote.KtorRiderService
import com.mobile.ride.domain.Vehicle
import com.mobile.ride.domain.VehicleType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorRiderServiceTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val sent = mutableListOf<RealtimeMessage>()

    private fun service(
        connection: ConnectionState = ConnectionState.CONNECTED,
        sendResult: EmptyResult<DataError.Connection> = Result.Success(Unit),
        status: HttpStatusCode = HttpStatusCode.NoContent,
        body: String = "",
    ) = KtorRiderService(
        httpClient = HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest { contentType(ContentType.Application.Json) }
        },
        realtimeClient = object : RealtimeClient {
            override val connectionState = MutableStateFlow(connection)
            override val messages = emptyFlow<RealtimeMessage>()
            override suspend fun send(message: RealtimeMessage): EmptyResult<DataError.Connection> {
                sent += message
                return sendResult
            }
        }
    )

    private val location = GeoPoint(24.9425, 67.114)

    @Test
    fun locationGoesOverTheSocketWhenConnected() = runTest {
        val result = service().shareLocation(location)

        assertTrue(result is Result.Success)
        assertEquals("LOCATION_UPDATE", sent.single().type)
        assertEquals("""{"latitude":24.9425,"longitude":67.114}""", sent.single().payload)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun locationFallsBackToRestWhileDisconnected() = runTest {
        service(connection = ConnectionState.CONNECTING).shareLocation(location)

        assertTrue(sent.isEmpty())
        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("/api/riders/me/location", requests.single().url.encodedPath)
    }

    @Test
    fun locationFallsBackToRestWhenTheSendFails() = runTest {
        service(sendResult = Result.Failure(DataError.Connection.MESSAGE_SEND_FAILED)).shareLocation(
            location
        )

        assertEquals(1, sent.size)
        assertEquals("/api/riders/me/location", requests.single().url.encodedPath)
    }

    @Test
    fun statusWithoutVehicle() = runTest {
        val result = service(
            status = HttpStatusCode.OK,
            body = """{"online":false,"vehicle":null}"""
        ).getStatus()

        assertEquals(null, (result as Result.Success).data.vehicle)
    }

    @Test
    fun saveVehicleSendsTheBackendShape() = runTest {
        val body =
            """{"vehicleType":"MOTO","make":"Honda","model":"CD 70","color":"Red","plateNumber":"KHI-123"}"""
        val result = service(status = HttpStatusCode.OK, body = body)
            .saveVehicle(Vehicle(VehicleType.MOTO, "Honda", "CD 70", "Red", "KHI-123"))

        assertEquals(VehicleType.MOTO, (result as Result.Success).data.type)
        assertEquals(body, requests.single().body.toByteArray().decodeToString())
    }

    @Test
    fun earningsSendTheDeviceTimeZone() = runTest {
        val body = """{"today":{"trips":3,"amount":1620},"last7Days":{"trips":12,"amount":6480}}"""
        val result = service(status = HttpStatusCode.OK, body = body).getEarnings()

        assertEquals(1620, (result as Result.Success).data.today.amount)
        assertEquals(12, result.data.last7Days.trips)
        val request = requests.single()
        assertEquals("/api/riders/me/earnings", request.url.encodedPath)
        assertEquals(request.url.parameters["zone"]?.isNotBlank(), true)
    }
}
