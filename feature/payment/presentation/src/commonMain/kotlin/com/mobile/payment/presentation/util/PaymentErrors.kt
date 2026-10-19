package com.mobile.payment.presentation.util

import com.mobile.core.domain.util.DataError
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.presentation.util.UiText
import com.mobile.core.presentation.util.toUiText
import com.mobile.payment.domain.CheckoutConfig
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.payment_unavailable

/** 503: the backend has no Stripe key configured. */
internal fun DataError.Remote.toPaymentUiText(): UiText = when (this) {
    DataError.Remote.SERVICE_UNAVAILABLE -> UiText.Resource(Res.string.payment_unavailable)
    else -> toUiText()
}

/** Network errors worth one retry with the same idempotency key. */
internal val DataError.Remote.isTransient: Boolean
    get() = this == DataError.Remote.NO_INTERNET ||
        this == DataError.Remote.REQUEST_TIMEOUT ||
        this == DataError.Remote.SERVER_ERROR

internal fun CheckoutConfig.toCheckoutSession(intent: CheckoutIntent) = CheckoutSession(
    intent = intent,
    publishableKey = publishableKey,
    customerId = customerId,
    customerSessionClientSecret = customerSessionClientSecret,
    merchantDisplayName = merchantDisplayName,
    merchantCountry = merchantCountry,
)
