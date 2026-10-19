package com.mobile.payment.presentation.methods

import com.mobile.core.payment.api.CheckoutResult

sealed interface PaymentMethodsAction {
    data object OnBackClick : PaymentMethodsAction
    data object OnRetryClick : PaymentMethodsAction
    data object OnAddCardClick : PaymentMethodsAction
    data object OnCancelAddCardClick : PaymentMethodsAction
    data class OnRemoveCardClick(val cardId: String) : PaymentMethodsAction
    data class OnCheckoutPrepared(val ready: Boolean, val errorMessage: String?) : PaymentMethodsAction
    data class OnCheckoutResult(val result: CheckoutResult) : PaymentMethodsAction
}
