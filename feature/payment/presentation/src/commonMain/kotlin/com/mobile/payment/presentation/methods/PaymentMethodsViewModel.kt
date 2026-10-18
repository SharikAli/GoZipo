package com.mobile.payment.presentation.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.util.Result
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.payment.api.PaymentUiMode
import com.mobile.core.presentation.util.UiText
import com.mobile.payment.domain.PaymentService
import com.mobile.payment.domain.newIdempotencyKey
import com.mobile.payment.presentation.util.isTransient
import com.mobile.payment.presentation.util.toCheckoutSession
import com.mobile.payment.presentation.util.toPaymentUiText
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.card_removed
import gozipo.feature.payment.presentation.generated.resources.card_saved
import gozipo.feature.payment.presentation.generated.resources.payment_load_failed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The customer's saved cards. "Add card" saves one with a SetupIntent (no charge), in the
 * build's payment UI, so later payments can use it.
 */
class PaymentMethodsViewModel(
    private val paymentService: PaymentService,
    private val paymentUiMode: PaymentUiMode,
) : ViewModel() {

    private val eventChannel = Channel<PaymentMethodsEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(PaymentMethodsState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadCards()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: PaymentMethodsAction) {
        when (action) {
            PaymentMethodsAction.OnRetryClick -> loadCards()
            PaymentMethodsAction.OnAddCardClick -> startAddCard()
            PaymentMethodsAction.OnCancelAddCardClick -> endAddCard()
            is PaymentMethodsAction.OnRemoveCardClick -> removeCard(action.cardId)
            is PaymentMethodsAction.OnCheckoutPrepared -> onCheckoutPrepared(
                action.ready,
                action.errorMessage
            )

            is PaymentMethodsAction.OnCheckoutResult -> onCheckoutResult(action.result)
            PaymentMethodsAction.OnBackClick -> Unit
        }
    }

    /** Stripe's UI asks for the SetupIntent when the customer saves the card. */
    suspend fun createIntent(@Suppress("UNUSED_PARAMETER") savePaymentMethod: Boolean): IntentCreation {
        val key = newIdempotencyKey()
        var result = paymentService.createSetupIntent(key)
        if (result is Result.Failure && result.error.isTransient) {
            result = paymentService.createSetupIntent(key)
        }
        return when (result) {
            is Result.Success -> IntentCreation.Created(result.data)
            is Result.Failure -> IntentCreation.Failed(
                result.error.toPaymentUiText().asStringAsync()
            )
        }
    }

    private fun loadCards() {
        _state.update { it.copy(isLoading = true, errorText = null) }
        viewModelScope.launch {
            when (val result = paymentService.getSavedCards()) {
                is Result.Success -> _state.update {
                    it.copy(
                        cards = result.data,
                        isLoading = false
                    )
                }

                is Result.Failure -> _state.update {
                    it.copy(isLoading = false, errorText = result.error.toPaymentUiText())
                }
            }
        }
    }

    private fun startAddCard() {
        if (_state.value.isAddingCard) return
        _state.update {
            it.copy(
                isAddingCard = true,
                isPreparingCheckout = true,
                isCheckoutReady = false
            )
        }
        viewModelScope.launch {
            when (val config = paymentService.getCheckoutConfig()) {
                is Result.Success -> eventChannel.send(
                    PaymentMethodsEvent.StartCheckout(
                        config.data.toCheckoutSession(CheckoutIntent.Setup(currency = config.data.currency))
                    )
                )

                is Result.Failure -> {
                    endAddCard()
                    eventChannel.send(PaymentMethodsEvent.ShowMessage(config.error.toPaymentUiText()))
                }
            }
        }
    }

    private fun onCheckoutPrepared(ready: Boolean, errorMessage: String?) {
        if (ready) {
            _state.update { it.copy(isPreparingCheckout = false, isCheckoutReady = true) }
        } else {
            endAddCard()
            viewModelScope.launch {
                eventChannel.send(
                    PaymentMethodsEvent.ShowMessage(
                        errorMessage?.let(UiText::DynamicString)
                            ?: UiText.Resource(Res.string.payment_load_failed)
                    )
                )
            }
        }
    }

    private fun onCheckoutResult(result: CheckoutResult) {
        when (result) {
            CheckoutResult.Completed -> {
                endAddCard()
                loadCards()
                viewModelScope.launch {
                    eventChannel.send(PaymentMethodsEvent.ShowMessage(UiText.Resource(Res.string.card_saved)))
                }
            }

            // The sheet closed; the inline UIs stay open until Cancel
            CheckoutResult.Canceled -> if (!paymentUiMode.hasOwnConfirmButton) endAddCard()

            is CheckoutResult.Failed -> viewModelScope.launch {
                result.message?.let {
                    eventChannel.send(PaymentMethodsEvent.ShowMessage(UiText.DynamicString(it)))
                }
            }
        }
    }

    private fun endAddCard() {
        _state.update {
            it.copy(
                isAddingCard = false,
                isPreparingCheckout = false,
                isCheckoutReady = false
            )
        }
    }

    private fun removeCard(cardId: String) {
        if (_state.value.removingCardId != null) return
        _state.update { it.copy(removingCardId = cardId) }
        viewModelScope.launch {
            when (val result = paymentService.removeCard(cardId)) {
                is Result.Success -> {
                    _state.update { state ->
                        state.copy(
                            cards = state.cards.filterNot { it.id == cardId },
                            removingCardId = null
                        )
                    }
                    eventChannel.send(PaymentMethodsEvent.ShowMessage(UiText.Resource(Res.string.card_removed)))
                }

                is Result.Failure -> {
                    _state.update { it.copy(removingCardId = null) }
                    eventChannel.send(PaymentMethodsEvent.ShowMessage(result.error.toPaymentUiText()))
                }
            }
        }
    }
}
