package com.mobile.core.payment.api

/** What the checkout collects: a payment of [amountMinor], or a card saved for later. */
sealed interface CheckoutIntent {
    /** @param amountMinor in the currency's minor unit (paisa for PKR), as the backend charges */
    data class Payment(val amountMinor: Long, val currency: String) : CheckoutIntent

    /** Saves a card without charging it (SetupIntent). */
    data class Setup(val currency: String) : CheckoutIntent
}

/**
 * Everything the provider's UI needs, from the backend. Secrets here are short-lived and
 * scoped to this customer; never log them.
 *
 * @param merchantCountry ISO country of the merchant account (Google Pay and Apple Pay)
 */
data class CheckoutSession(
    val intent: CheckoutIntent,
    val publishableKey: String,
    val customerId: String,
    val customerSessionClientSecret: String,
    val merchantDisplayName: String,
    val merchantCountry: String,
)

/** The backend's answer when the provider asks for the intent at confirmation time. */
sealed interface IntentCreation {
    data class Created(val clientSecret: String) : IntentCreation

    /** @param message shown in the provider's UI */
    data class Failed(val message: String) : IntentCreation
}

/**
 * How a confirmation ended, from the customer's side. [Completed] doesn't mean money moved:
 * the backend's status (from the provider's webhook) is the truth.
 */
sealed interface CheckoutResult {
    data object Completed : CheckoutResult
    data object Canceled : CheckoutResult
    data class Failed(val message: String?) : CheckoutResult
}

/** The method the customer picked, e.g. "•••• 4242" or "Google Pay" (flow controller, embedded). */
data class PaymentOption(
    val label: String
)

/**
 * @param isReady configured with a [CheckoutSession]; [CheckoutController.confirm] works
 * @param option the selected method, when the mode shows one
 * @param errorMessage why the last [CheckoutController.prepare] failed
 */
data class CheckoutState(
    val isPreparing: Boolean = false,
    val isReady: Boolean = false,
    val isConfirming: Boolean = false,
    val option: PaymentOption? = null,
    val errorMessage: String? = null,
)
