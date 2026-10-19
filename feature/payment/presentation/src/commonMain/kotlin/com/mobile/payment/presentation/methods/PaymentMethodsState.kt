package com.mobile.payment.presentation.methods

import com.mobile.core.presentation.util.UiText
import com.mobile.payment.domain.SavedCard

/**
 * @param isAddingCard a SetupIntent checkout is being prepared or shown
 * @param isCheckoutReady the inline add-card UI (flow controller, embedded) is ready
 */
data class PaymentMethodsState(
    val cards: List<SavedCard> = emptyList(),
    val isLoading: Boolean = true,
    val isAddingCard: Boolean = false,
    val isPreparingCheckout: Boolean = false,
    val isCheckoutReady: Boolean = false,
    val removingCardId: String? = null,
    val errorText: UiText? = null,
)
