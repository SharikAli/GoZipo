package com.mobile.payment.presentation.ride

import com.mobile.core.payment.api.CheckoutResult

sealed interface RidePaymentAction {
    data object OnPayWithCardClick : RidePaymentAction
    data object OnPayInCashClick : RidePaymentAction
    data object OnCheckAgainClick : RidePaymentAction

    /** The payment UI is (or isn't) ready, after [RidePaymentEvent.StartCheckout]. */
    data class OnCheckoutPrepared(val ready: Boolean, val errorMessage: String?) : RidePaymentAction
    data class OnCheckoutResult(val result: CheckoutResult) : RidePaymentAction
}
