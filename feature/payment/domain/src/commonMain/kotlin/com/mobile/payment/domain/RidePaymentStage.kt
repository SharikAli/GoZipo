package com.mobile.payment.domain

enum class RidePaymentStage {
    /** "Pay with card" (or nothing, when card payments are off for this ride). */
    IDLE,

    /** Loading the customer's checkout session and saved methods. */
    PREPARING,

    /** Payment UI ready: method row / inline methods and the pay button (sheet: presented). */
    CHECKOUT,

    /** Stripe's UI finished; waiting for the backend (webhook) to confirm. */
    VERIFYING,

    PAID,
}