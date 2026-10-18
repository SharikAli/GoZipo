package com.mobile.payment.data.dto

import com.mobile.payment.domain.CheckoutConfig
import com.mobile.payment.domain.RidePayment
import com.mobile.payment.domain.RidePaymentStatus
import com.mobile.payment.domain.RidePaymentUpdate
import com.mobile.payment.domain.SavedCard

internal fun CheckoutConfigDto.toCheckoutConfig() = CheckoutConfig(
    publishableKey = publishableKey,
    customerId = customerId,
    customerSessionClientSecret = customerSessionClientSecret,
    merchantDisplayName = merchantDisplayName,
    merchantCountry = merchantCountry,
    currency = currency,
)

internal fun RidePaymentDto.toRidePayment() = RidePayment(
    rideId = rideId,
    status = status.toPaymentStatus(),
    amount = amount,
    amountMinor = amountMinor,
    currency = currency,
    payable = payable,
    failureMessage = failureMessage,
)

internal fun SavedCardDto.toSavedCard() = SavedCard(
    id = id,
    brand = brand,
    last4 = last4,
    expMonth = expMonth,
    expYear = expYear,
)

internal fun RidePaymentUpdateDto.toRidePaymentUpdate() = RidePaymentUpdate(
    rideId = rideId,
    status = status.toPaymentStatus(),
    amount = amount,
)

/** Unknown statuses from a newer backend read as "not paid yet", never as paid. */
internal fun String.toPaymentStatus(): RidePaymentStatus =
    RidePaymentStatus.entries.firstOrNull { it.name == this } ?: RidePaymentStatus.REQUIRES_PAYMENT
