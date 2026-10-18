package com.mobile.payment.domain

import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result

/**
 * Customer card payments (`/payments`). Creating calls take an idempotency key: one per
 * attempt (one tap of Pay or Save), reused when that same call is retried, so a retry never
 * creates a second intent.
 */
interface PaymentService {
    suspend fun getCheckoutConfig(): Result<CheckoutConfig, DataError.Remote>

    /** @return the SetupIntent's client secret (saves a card without charging it) */
    suspend fun createSetupIntent(idempotencyKey: String): Result<String, DataError.Remote>

    suspend fun getRidePayment(rideId: String): Result<RidePayment, DataError.Remote>

    /**
     * @return the ride's PaymentIntent client secret. CONFLICT means it's already paid,
     * still processing or not payable: read [getRidePayment] for which.
     */
    suspend fun createRidePaymentIntent(
        rideId: String,
        savePaymentMethod: Boolean,
        idempotencyKey: String
    ): Result<String, DataError.Remote>

    suspend fun getSavedCards(): Result<List<SavedCard>, DataError.Remote>

    suspend fun removeCard(cardId: String): EmptyResult<DataError.Remote>
}
