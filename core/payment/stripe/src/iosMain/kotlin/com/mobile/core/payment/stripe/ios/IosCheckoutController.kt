package com.mobile.core.payment.stripe.ios

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.mobile.core.payment.api.CheckoutController
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.payment.api.PaymentOption
import com.mobile.core.payment.api.PaymentUiMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIView
import kotlin.coroutines.resume

internal class IosCheckoutController(
    override val mode: PaymentUiMode,
    private val scope: CoroutineScope,
    private val requestIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
    private val deliverResult: (CheckoutResult) -> Unit,
) : CheckoutController {

    lateinit var checkout: GoZipoStripeCheckout

    private val mutableState = MutableStateFlow(CheckoutState())
    override val state: StateFlow<CheckoutState> = mutableState.asStateFlow()

    private var embeddedHeight by mutableDoubleStateOf(0.0)
    private var embeddedView by mutableStateOf<UIView?>(null)

    val listener = object : GoZipoStripeListener {
        override fun createIntent(
            savePaymentMethod: Boolean,
            completion: (clientSecret: String?, errorMessage: String?) -> Unit
        ) {
            scope.launch {
                when (val created = requestIntent(savePaymentMethod)) {
                    is IntentCreation.Created -> completion(created.clientSecret, null)
                    is IntentCreation.Failed -> completion(null, created.message)
                }
            }
        }

        override fun onPaymentOptionChanged(label: String?) {
            mutableState.update { it.copy(option = label?.let(::PaymentOption)) }
        }

        override fun onResult(status: String, errorMessage: String?) {
            mutableState.update { it.copy(isConfirming = false) }
            deliverResult(
                when (status) {
                    "completed" -> CheckoutResult.Completed
                    "canceled" -> CheckoutResult.Canceled
                    else -> CheckoutResult.Failed(errorMessage)
                }
            )
        }

        override fun onEmbeddedHeightChanged(height: Double) {
            embeddedHeight = height
        }
    }

    override suspend fun prepare(session: CheckoutSession): Boolean {
        mutableState.update { it.copy(isPreparing = true, isReady = false, errorMessage = null) }
        val error = suspendCancellableCoroutine { continuation ->
            checkout.prepare(session.toBridgeConfig()) { error ->
                if (continuation.isActive) continuation.resume(error)
            }
        }
        if (error == null && mode == PaymentUiMode.EMBEDDED) {
            embeddedView = checkout.embeddedView()
        }
        mutableState.update {
            it.copy(
                isPreparing = false,
                isReady = error == null,
                errorMessage = error
            )
        }
        return error == null
    }

    override fun changePaymentOption() {
        if (mode == PaymentUiMode.FLOW_CONTROLLER && state.value.isReady) checkout.presentPaymentOptions()
    }

    override fun confirm() {
        if (!state.value.isReady) return
        if (mode != PaymentUiMode.PAYMENT_SHEET && state.value.option == null) {
            changePaymentOption()
            return
        }
        mutableState.update { it.copy(isConfirming = true) }
        checkout.confirm()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val view = embeddedView ?: return
        if (mode != PaymentUiMode.EMBEDDED) return
        UIKitView(
            factory = { view },
            modifier = modifier
                .fillMaxWidth()
                .height(embeddedHeight.dp),
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true)
        )
    }

    private fun CheckoutSession.toBridgeConfig(): GoZipoStripeConfig {
        val payment = intent as? CheckoutIntent.Payment
        return GoZipoStripeConfig(
            publishableKey = publishableKey,
            customerId = customerId,
            customerSessionClientSecret = customerSessionClientSecret,
            merchantDisplayName = merchantDisplayName,
            merchantCountry = merchantCountry,
            currency = payment?.currency ?: (intent as CheckoutIntent.Setup).currency,
            amountMinor = payment?.amountMinor ?: 0L,
            setup = intent is CheckoutIntent.Setup,
        )
    }
}

