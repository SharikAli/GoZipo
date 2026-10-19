package com.mobile.payment.domain

/**
 * A ride's card payment, as the backend reports it (from Stripe's webhooks, never from what
 * the app saw). [amount] is whole PKR; [amountMinor] is what is charged (paisa).
 *
 * @param payable the customer can start or retry a card payment now
 * @param failureMessage the card's decline reason, when [status] is FAILED
 */
data class RidePayment(
    val rideId: String,
    val status: RidePaymentStatus,
    val amount: Int,
    val amountMinor: Long,
    val currency: String,
    val payable: Boolean,
    val failureMessage: String?,
)

enum class RidePaymentStatus {
    REQUIRES_PAYMENT,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    REFUNDED;

    val isPaid: Boolean
        get() = this == SUCCEEDED
}

/**
 * What the app needs to open the payment UI for the signed-in customer. The secret is
 * short-lived and scoped to this customer.
 */
data class CheckoutConfig(
    val publishableKey: String,
    val customerId: String,
    val customerSessionClientSecret: String,
    val merchantDisplayName: String,
    val merchantCountry: String,
    val currency: String,
)

/** A card saved on the customer's account. */
data class SavedCard(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)

/** A realtime change to a ride's card payment (`PAYMENT_UPDATED`). */
data class RidePaymentUpdate(
    val rideId: String,
    val status: RidePaymentStatus,
    val amount: Int,
)
