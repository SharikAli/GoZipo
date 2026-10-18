package com.mobile.payment.presentation.ride

import com.mobile.core.presentation.util.UiText
import com.mobile.payment.domain.RidePaymentStage

/**
 * @param amount whole PKR, for display
 * @param payable a card payment can be started now (from the backend)
 * @param stillProcessing verification timed out with the payment still processing
 */
data class RidePaymentState(
    val rideId: String = "",
    val amount: Int = 0,
    val stage: RidePaymentStage = RidePaymentStage.IDLE,
    val payable: Boolean = false,
    val stillProcessing: Boolean = false,
    val refunded: Boolean = false,
    val errorText: UiText? = null,
)
