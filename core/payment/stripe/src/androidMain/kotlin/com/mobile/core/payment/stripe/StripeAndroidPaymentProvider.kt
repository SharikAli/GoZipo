package com.mobile.core.payment.stripe

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mobile.core.payment.api.CheckoutController
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.payment.api.PaymentProvider
import com.mobile.core.payment.api.PaymentUiMode
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Stripe's Android payment UIs. All three use a deferred intent: the UI is configured with
 * the amount (or setup) and the customer, and Stripe asks for the intent's client secret
 * (`createIntent`) only when the customer confirms.
 */
internal class StripeAndroidPaymentProvider : PaymentProvider {

    @Composable
    override fun rememberCheckoutController(
        mode: PaymentUiMode,
        createIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController {
        val context = LocalContext.current.applicationContext
        val createIntentCallback = remember {
            CreateIntentCallback { _, shouldSavePaymentMethod ->
                when (val created = createIntent(shouldSavePaymentMethod)) {
                    is IntentCreation.Created -> CreateIntentResult.Success(created.clientSecret)
                    is IntentCreation.Failed -> CreateIntentResult.Failure(
                        cause = IllegalStateException(created.message),
                        displayMessage = created.message
                    )
                }
            }
        }
        return when (mode) {
            PaymentUiMode.PAYMENT_SHEET -> rememberSheetController(context, createIntentCallback, onResult)
            PaymentUiMode.FLOW_CONTROLLER -> rememberFlowController(context, createIntentCallback, onResult)
            PaymentUiMode.EMBEDDED -> rememberEmbeddedController(context, createIntentCallback, onResult)
        }
    }

    @Composable
    private fun rememberSheetController(
        context: Context,
        createIntentCallback: CreateIntentCallback,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController {
        val controller = remember { SheetController(context, onResult) }
        val sheet = remember {
            PaymentSheet.Builder(controller::onSheetResult)
                .createIntentCallback(createIntentCallback)
        }.build()
        controller.sheet = sheet
        return controller
    }

    @Composable
    private fun rememberFlowController(
        context: Context,
        createIntentCallback: CreateIntentCallback,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController {
        val controller = remember { FlowController(context, onResult) }
        val flowController = remember {
            PaymentSheet.FlowController.Builder(
                resultCallback = controller::onSheetResult,
                paymentOptionCallback = { option -> controller.onOption(option?.label) }
            ).createIntentCallback(createIntentCallback)
        }.build()
        controller.flowController = flowController
        return controller
    }

    @Composable
    private fun rememberEmbeddedController(
        context: Context,
        createIntentCallback: CreateIntentCallback,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController {
        val controller = remember { EmbeddedController(context, onResult) }
        val builder = remember {
            EmbeddedPaymentElement.Builder(
                createIntentCallback = createIntentCallback,
                resultCallback = controller::onEmbeddedResult
            )
        }
        val element = rememberEmbeddedPaymentElement(builder)
        controller.element = element
        LaunchedEffect(element) {
            element.paymentOption.collect { option -> controller.onOption(option?.label) }
        }
        return controller
    }
}
