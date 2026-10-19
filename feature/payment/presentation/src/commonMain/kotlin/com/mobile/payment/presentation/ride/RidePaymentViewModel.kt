package com.mobile.payment.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.presentation.util.UiText
import com.mobile.payment.domain.PaymentEventSource
import com.mobile.payment.domain.PaymentService
import com.mobile.payment.domain.RidePayment
import com.mobile.payment.domain.RidePaymentStage
import com.mobile.payment.domain.RidePaymentStatus
import com.mobile.payment.domain.newIdempotencyKey
import com.mobile.payment.presentation.util.isTransient
import com.mobile.payment.presentation.util.toCheckoutSession
import com.mobile.payment.presentation.util.toPaymentUiText
import gozipo.feature.payment.presentation.generated.resources.Res
import gozipo.feature.payment.presentation.generated.resources.payment_already_paid
import gozipo.feature.payment.presentation.generated.resources.payment_failed
import gozipo.feature.payment.presentation.generated.resources.payment_load_failed
import gozipo.feature.payment.presentation.generated.resources.payment_not_payable
import gozipo.feature.payment.presentation.generated.resources.payment_processing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * "Pay with card" for one finished ride. Cash stays the default: nothing here changes the ride.
 *
 * Paid means the backend says SUCCEEDED (Stripe's webhook), not that Stripe's UI said
 * completed: after the UI completes, this listens for `PAYMENT_UPDATED` and polls until the
 * backend confirms.
 *
 * @param fare whole PKR, shown until the backend's amount arrives
 */
class RidePaymentViewModel(
    private val rideId: String,
    fare: Int,
    private val paymentService: PaymentService,
    private val paymentEventSource: PaymentEventSource,
) : ViewModel() {

    private companion object {
        val VERIFY_POLL_INTERVAL = 2.seconds
        const val VERIFY_POLL_ATTEMPTS = 5
    }

    private val _events = MutableSharedFlow<RidePaymentEvent>()
    val events = _events.asSharedFlow()

    private var hasLoadedInitialData = false
    private var verifyJob: Job? = null

    private val _state = MutableStateFlow(RidePaymentState(rideId = rideId, amount = fare))
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadPayment()
                observeUpdates()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: RidePaymentAction) {
        when (action) {
            RidePaymentAction.OnPayWithCardClick -> startCheckout()
            RidePaymentAction.OnPayInCashClick -> _state.update {
                it.copy(stage = RidePaymentStage.IDLE, errorText = null)
            }

            RidePaymentAction.OnCheckAgainClick -> verify()
            is RidePaymentAction.OnCheckoutPrepared -> onCheckoutPrepared(
                action.ready,
                action.errorMessage
            )

            is RidePaymentAction.OnCheckoutResult -> onCheckoutResult(action.result)
        }
    }

    /**
     * Stripe's UI asks for the PaymentIntent when the customer confirms. One key per
     * confirmation, reused for a single retry of the same call. The backend reuses the ride's
     * intent anyway, so the ride is never charged twice.
     */
    suspend fun createIntent(savePaymentMethod: Boolean): IntentCreation {
        val key = newIdempotencyKey()
        var result = paymentService.createRidePaymentIntent(rideId, savePaymentMethod, key)
        if (result is Result.Failure && result.error.isTransient) {
            result = paymentService.createRidePaymentIntent(rideId, savePaymentMethod, key)
        }
        return when (result) {
            is Result.Success -> IntentCreation.Created(result.data)
            is Result.Failure -> {
                val text = if (result.error == DataError.Remote.CONFLICT) {
                    // Paid, processing or not payable: the backend's status says which
                    conflictText()
                } else {
                    result.error.toPaymentUiText()
                }
                IntentCreation.Failed(text.asStringAsync())
            }
        }
    }

    private fun loadPayment() {
        viewModelScope.launch {
            paymentService.getRidePayment(rideId).onSuccess(::applyPayment)
        }
    }

    private fun observeUpdates() {
        paymentEventSource.updates
            .filter { it.rideId == rideId }
            .onEach { update ->
                when (update.status) {
                    RidePaymentStatus.SUCCEEDED -> markPaid()
                    RidePaymentStatus.FAILED -> if (_state.value.stage == RidePaymentStage.VERIFYING) {
                        // The reason comes with the payment
                        verify()
                    }

                    RidePaymentStatus.REFUNDED -> _state.update {
                        it.copy(stage = RidePaymentStage.IDLE, refunded = true, payable = false)
                    }

                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startCheckout() {
        if (_state.value.stage != RidePaymentStage.IDLE) return
        _state.update { it.copy(stage = RidePaymentStage.PREPARING, errorText = null) }
        viewModelScope.launch {
            val payment = paymentService.getRidePayment(rideId)
            if (payment is Result.Failure) {
                failCheckout(payment.error.toPaymentUiText())
                return@launch
            }
            val ridePayment = (payment as Result.Success).data
            applyPayment(ridePayment)
            when {
                ridePayment.status.isPaid -> return@launch
                ridePayment.status == RidePaymentStatus.PROCESSING -> {
                    verify()
                    return@launch
                }

                !ridePayment.payable -> {
                    failCheckout(UiText.Resource(Res.string.payment_not_payable))
                    return@launch
                }
            }

            when (val config = paymentService.getCheckoutConfig()) {
                is Result.Success -> _events.emit(
                    RidePaymentEvent.StartCheckout(
                        config.data.toCheckoutSession(
                            CheckoutIntent.Payment(
                                amountMinor = ridePayment.amountMinor,
                                currency = ridePayment.currency
                            )
                        )
                    )
                )

                is Result.Failure -> failCheckout(config.error.toPaymentUiText())
            }
        }
    }

    private fun onCheckoutPrepared(ready: Boolean, errorMessage: String?) {
        if (ready) {
            _state.update { it.copy(stage = RidePaymentStage.CHECKOUT, errorText = null) }
        } else {
            failCheckout(
                errorMessage?.let(UiText::DynamicString)
                    ?: UiText.Resource(Res.string.payment_load_failed)
            )
        }
    }

    private fun onCheckoutResult(result: CheckoutResult) {
        when (result) {
            CheckoutResult.Completed -> verify()

            // The sheet closed; the inline UIs stay so the customer can pick again
            CheckoutResult.Canceled -> _state.update {
                if (it.stage == RidePaymentStage.CHECKOUT) it else it.copy(stage = RidePaymentStage.IDLE)
            }

            is CheckoutResult.Failed -> _state.update {
                it.copy(
                    errorText = result.message?.let(UiText::DynamicString)
                        ?: UiText.Resource(Res.string.payment_failed)
                )
            }
        }
    }

    /** Waits for the backend to confirm (webhook), polling in case the realtime update is missed. */
    private fun verify() {
        verifyJob?.cancel()
        _state.update {
            it.copy(stage = RidePaymentStage.VERIFYING, stillProcessing = false, errorText = null)
        }
        verifyJob = viewModelScope.launch {
            repeat(VERIFY_POLL_ATTEMPTS) {
                val result = paymentService.getRidePayment(rideId)
                if (result is Result.Success) {
                    val payment = result.data
                    when (payment.status) {
                        RidePaymentStatus.SUCCEEDED -> {
                            markPaid()
                            return@launch
                        }

                        RidePaymentStatus.FAILED, RidePaymentStatus.CANCELED -> {
                            applyPayment(payment)
                            _state.update {
                                it.copy(
                                    stage = RidePaymentStage.IDLE,
                                    errorText = payment.failureMessage?.let(UiText::DynamicString)
                                        ?: UiText.Resource(Res.string.payment_failed)
                                )
                            }
                            return@launch
                        }

                        else -> Unit
                    }
                }
                delay(VERIFY_POLL_INTERVAL)
            }
            _state.update { it.copy(stillProcessing = true) }
        }
    }

    private fun applyPayment(payment: RidePayment) {
        _state.update {
            it.copy(
                amount = payment.amount.takeIf { amount -> amount > 0 } ?: it.amount,
                payable = payment.payable,
                refunded = payment.status == RidePaymentStatus.REFUNDED,
                stage = if (payment.status.isPaid) RidePaymentStage.PAID else it.stage,
            )
        }
    }

    private fun markPaid() {
        verifyJob?.cancel()
        _state.update {
            it.copy(
                stage = RidePaymentStage.PAID,
                payable = false,
                stillProcessing = false,
                errorText = null
            )
        }
    }

    private fun failCheckout(text: UiText) {
        _state.update { it.copy(stage = RidePaymentStage.IDLE, errorText = text) }
    }

    private suspend fun conflictText(): UiText {
        val payment = (paymentService.getRidePayment(rideId) as? Result.Success)?.data
        payment?.let(::applyPayment)
        return when {
            payment?.status?.isPaid == true -> UiText.Resource(Res.string.payment_already_paid)
            payment?.status == RidePaymentStatus.PROCESSING -> {
                verify()
                UiText.Resource(Res.string.payment_processing)
            }

            else -> UiText.Resource(Res.string.payment_not_payable)
        }
    }
}
