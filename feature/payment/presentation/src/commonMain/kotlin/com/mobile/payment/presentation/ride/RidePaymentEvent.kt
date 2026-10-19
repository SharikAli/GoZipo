package com.mobile.payment.presentation.ride

import com.mobile.core.payment.api.CheckoutSession

sealed interface RidePaymentEvent {
    /** Prepare the screen's checkout with this session (and present it, for the sheet). */
    data class StartCheckout(val session: CheckoutSession) : RidePaymentEvent
}
