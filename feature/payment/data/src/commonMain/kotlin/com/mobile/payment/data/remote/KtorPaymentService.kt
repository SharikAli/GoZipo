package com.mobile.payment.data.remote

import com.mobile.core.data.network.delete
import com.mobile.core.data.network.get
import com.mobile.core.data.network.post
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import com.mobile.payment.data.dto.CheckoutConfigDto
import com.mobile.payment.data.dto.CreatePaymentIntentRequestDto
import com.mobile.payment.data.dto.EmptyRequestDto
import com.mobile.payment.data.dto.PaymentIntentDto
import com.mobile.payment.data.dto.RidePaymentDto
import com.mobile.payment.data.dto.SavedCardDto
import com.mobile.payment.data.dto.SetupIntentDto
import com.mobile.payment.data.dto.toCheckoutConfig
import com.mobile.payment.data.dto.toRidePayment
import com.mobile.payment.data.dto.toSavedCard
import com.mobile.payment.domain.CheckoutConfig
import com.mobile.payment.domain.PaymentService
import com.mobile.payment.domain.RidePayment
import com.mobile.payment.domain.SavedCard
import io.ktor.client.HttpClient
import io.ktor.client.request.header

class KtorPaymentService(
    private val httpClient: HttpClient
) : PaymentService {

    private companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    }

    override suspend fun getCheckoutConfig(): Result<CheckoutConfig, DataError.Remote> {
        return httpClient.post<EmptyRequestDto, CheckoutConfigDto>(
            route = "/payments/checkout-config",
            body = EmptyRequestDto()
        ).map { it.toCheckoutConfig() }
    }

    override suspend fun createSetupIntent(idempotencyKey: String): Result<String, DataError.Remote> {
        return httpClient.post<EmptyRequestDto, SetupIntentDto>(
            route = "/payments/setup-intents",
            body = EmptyRequestDto()
        ) {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
        }.map { it.clientSecret }
    }

    override suspend fun getRidePayment(rideId: String): Result<RidePayment, DataError.Remote> {
        return httpClient.get<RidePaymentDto>(
            route = "/payments/rides/$rideId"
        ).map { it.toRidePayment() }
    }

    override suspend fun createRidePaymentIntent(
        rideId: String,
        savePaymentMethod: Boolean,
        idempotencyKey: String
    ): Result<String, DataError.Remote> {
        return httpClient.post<CreatePaymentIntentRequestDto, PaymentIntentDto>(
            route = "/payments/rides/$rideId/intent",
            body = CreatePaymentIntentRequestDto(savePaymentMethod)
        ) {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
        }.map { it.clientSecret }
    }

    override suspend fun getSavedCards(): Result<List<SavedCard>, DataError.Remote> {
        return httpClient.get<List<SavedCardDto>>(
            route = "/payments/cards"
        ).map { cards -> cards.map { it.toSavedCard() } }
    }

    override suspend fun removeCard(cardId: String): EmptyResult<DataError.Remote> {
        return httpClient.delete<Unit>(
            route = "/payments/cards/$cardId"
        )
    }
}
