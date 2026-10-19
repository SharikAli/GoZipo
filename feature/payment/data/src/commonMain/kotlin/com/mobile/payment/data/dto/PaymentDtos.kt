package com.mobile.payment.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutConfigDto(
    val publishableKey: String,
    val customerId: String,
    val customerSessionClientSecret: String,
    val merchantDisplayName: String,
    val merchantCountry: String,
    val currency: String,
)

@Serializable
data class SetupIntentDto(
    val clientSecret: String
)

@Serializable
data class RidePaymentDto(
    val rideId: String,
    val status: String,
    val amount: Int,
    val amountMinor: Long,
    val currency: String,
    val payable: Boolean,
    val failureMessage: String? = null,
)

@Serializable
data class CreatePaymentIntentRequestDto(
    val savePaymentMethod: Boolean
)

@Serializable
data class PaymentIntentDto(
    val paymentId: String,
    val clientSecret: String,
    val status: String,
)

@Serializable
data class SavedCardDto(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)

/** `PAYMENT_UPDATED` realtime payload. */
@Serializable
data class RidePaymentUpdateDto(
    val rideId: String,
    val status: String,
    val amount: Int,
    val currency: String,
)

@Serializable
class EmptyRequestDto
