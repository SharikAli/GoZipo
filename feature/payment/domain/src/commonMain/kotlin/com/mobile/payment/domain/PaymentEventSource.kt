package com.mobile.payment.domain

import kotlinx.coroutines.flow.Flow

/** Card payment changes pushed over the realtime socket (after Stripe's webhook). */
interface PaymentEventSource {
    val updates: Flow<RidePaymentUpdate>
}
