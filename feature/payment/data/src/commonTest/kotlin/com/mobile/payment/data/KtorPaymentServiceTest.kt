package com.mobile.payment.data

import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.payment.data.remote.KtorPaymentService
import com.mobile.payment.domain.RidePaymentStatus
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorPaymentServiceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun service(status: HttpStatusCode, body: String) = KtorPaymentService(
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
    fun paymentIntentSendsTheIdempotencyKeyAndSaveChoice() = runTest {
        val result = service(
            HttpStatusCode.OK,
            """{"paymentId":"p1","clientSecret":"pi_1_secret_x","status":"REQUIRES_PAYMENT"}"""
        ).createRidePaymentIntent("ride-1", savePaymentMethod = true, idempotencyKey = "key-0001")

        assertEquals("pi_1_secret_x", (result as Result.Success).data)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/payments/rides/ride-1/intent", request.url.encodedPath)
        assertEquals("key-0001", request.headers["Idempotency-Key"])
        assertEquals("""{"savePaymentMethod":true}""", request.body.toByteArray().decodeToString())
    }

    @Test
    fun alreadyPaidRideIsAConflict() = runTest {
        val result = service(
            HttpStatusCode.Conflict,
            """{"code":"ALREADY_PAID","message":"This ride is already paid"}"""
        ).createRidePaymentIntent("ride-1", savePaymentMethod = false, idempotencyKey = "key-0002")

        assertEquals(DataError.Remote.CONFLICT, (result as Result.Failure).error)
    }

    @Test
    fun setupIntentSendsTheIdempotencyKey() = runTest {
        val result = service(HttpStatusCode.Created, """{"clientSecret":"seti_1_secret_x"}""")
            .createSetupIntent("key-0003")

        assertEquals("seti_1_secret_x", (result as Result.Success).data)
        assertEquals("/api/payments/setup-intents", requests.single().url.encodedPath)
        assertEquals("key-0003", requests.single().headers["Idempotency-Key"])
    }

    @Test
    fun ridePaymentIsDecoded() = runTest {
        val result = service(
            HttpStatusCode.OK,
            """
            {"rideId":"ride-1","status":"FAILED","amount":450,"amountMinor":45000,"currency":"pkr",
             "payable":true,"failureMessage":"Your card was declined."}
            """
        ).getRidePayment("ride-1")

        val payment = (result as Result.Success).data
        assertEquals(RidePaymentStatus.FAILED, payment.status)
        assertEquals(45_000, payment.amountMinor)
        assertTrue(payment.payable)
        assertEquals("Your card was declined.", payment.failureMessage)
    }

    @Test
    fun unknownStatusIsNeverReadAsPaid() = runTest {
        val result = service(
            HttpStatusCode.OK,
            """{"rideId":"ride-1","status":"SOMETHING_NEW","amount":450,"amountMinor":45000,"currency":"pkr","payable":false}"""
        ).getRidePayment("ride-1")

        assertEquals(RidePaymentStatus.REQUIRES_PAYMENT, (result as Result.Success).data.status)
    }

    @Test
    fun savedCardsAreDecoded() = runTest {
        val result = service(
            HttpStatusCode.OK,
            """[{"id":"pm_1","brand":"visa","last4":"4242","expMonth":12,"expYear":2030}]"""
        ).getSavedCards()

        val card = (result as Result.Success).data.single()
        assertEquals("4242", card.last4)
        assertEquals("/api/payments/cards", requests.single().url.encodedPath)
    }
}
