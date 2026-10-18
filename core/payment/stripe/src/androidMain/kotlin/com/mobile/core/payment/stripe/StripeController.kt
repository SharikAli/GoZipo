package com.mobile.core.payment.stripe

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.core.payment.api.CheckoutController
import com.mobile.core.payment.api.CheckoutIntent
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.PaymentOption
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared state and Stripe configuration for the three controllers. */
internal abstract class StripeController(
    private val context: Context,
    private val onResult: (CheckoutResult) -> Unit,
) : CheckoutController {

    protected val mutableState = MutableStateFlow(CheckoutState())
    override val state: StateFlow<CheckoutState> = mutableState.asStateFlow()

    protected var session: CheckoutSession? = null

    override suspend fun prepare(session: CheckoutSession): Boolean {
        this.session = session
        PaymentConfiguration.init(context, session.publishableKey)
        mutableState.update { it.copy(isPreparing = true, isReady = false, errorMessage = null) }
        val error = configure(session)
        mutableState.update {
            it.copy(isPreparing = false, isReady = error == null, errorMessage = error)
        }
        return error == null
    }

    /** @return null on success, else a message to show */
    protected abstract suspend fun configure(session: CheckoutSession): String?

    override fun changePaymentOption() = Unit

    @Composable
    override fun Content(modifier: Modifier) = Unit

    fun onOption(label: String?) {
        mutableState.update { it.copy(option = label?.let(::PaymentOption)) }
    }

    fun onSheetResult(result: PaymentSheetResult) {
        finish(
            when (result) {
                is PaymentSheetResult.Completed -> CheckoutResult.Completed
                is PaymentSheetResult.Canceled -> CheckoutResult.Canceled
                is PaymentSheetResult.Failed -> CheckoutResult.Failed(result.error.localizedMessage)
            }
        )
    }

    fun onEmbeddedResult(result: EmbeddedPaymentElement.Result) {
        finish(
            when (result) {
                is EmbeddedPaymentElement.Result.Completed -> CheckoutResult.Completed
                is EmbeddedPaymentElement.Result.Canceled -> CheckoutResult.Canceled
                is EmbeddedPaymentElement.Result.Failed -> CheckoutResult.Failed(result.error.localizedMessage)
            }
        )
    }

    protected fun startConfirming(): CheckoutSession? {
        val session = session?.takeIf { state.value.isReady } ?: return null
        mutableState.update { it.copy(isConfirming = true) }
        return session
    }

    private fun finish(result: CheckoutResult) {
        mutableState.update { it.copy(isConfirming = false) }
        onResult(result)
    }

    protected fun CheckoutSession.intentConfiguration(): PaymentSheet.IntentConfiguration {
        val mode = when (val intent = intent) {
            is CheckoutIntent.Payment -> PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = intent.amountMinor,
                currency = intent.currency
            )

            is CheckoutIntent.Setup -> PaymentSheet.IntentConfiguration.Mode.Setup(
                currency = intent.currency,
                setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
            )
        }
        return PaymentSheet.IntentConfiguration(
            mode = mode,
            paymentMethodTypes = listOf(StripeConfig.PAYMENT_METHOD_CARD)
        )
    }

    protected fun CheckoutSession.customerConfiguration() =
        PaymentSheet.CustomerConfiguration.createWithCustomerSession(
            id = customerId,
            clientSecret = customerSessionClientSecret
        )

    protected fun CheckoutSession.googlePayConfiguration(): PaymentSheet.GooglePayConfiguration {
        val payment = intent as? CheckoutIntent.Payment
        return PaymentSheet.GooglePayConfiguration(
            environment = if (StripeConfig.isLiveKey(publishableKey)) {
                PaymentSheet.GooglePayConfiguration.Environment.Production
            } else {
                PaymentSheet.GooglePayConfiguration.Environment.Test
            },
            countryCode = merchantCountry,
            currencyCode = (payment?.currency
                ?: (intent as CheckoutIntent.Setup).currency).uppercase(),
            amount = payment?.amountMinor,
        )
    }

    /** Visa and Mastercard only. */
    protected val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance =
        PaymentSheet.CardBrandAcceptance.allowed(
            listOf(
                PaymentSheet.CardBrandAcceptance.BrandCategory.Visa,
                PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard,
            )
        )

    protected fun CheckoutSession.sheetConfiguration(): PaymentSheet.Configuration =
        PaymentSheet.Configuration.Builder(merchantDisplayName)
            .customer(customerConfiguration())
            .googlePay(googlePayConfiguration())
            .cardBrandAcceptance(cardBrandAcceptance)
            .allowsDelayedPaymentMethods(false)
            .build()
}