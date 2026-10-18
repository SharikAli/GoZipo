package com.mobile.payment.presentation.methods

import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.presentation.util.UiText

sealed interface PaymentMethodsEvent {
    data class StartCheckout(val session: CheckoutSession) : PaymentMethodsEvent
    data class ShowMessage(val message: UiText) : PaymentMethodsEvent
}
