package com.mobile.core.payment.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

/**
 * The app's checkout. Features use only this; the SDK comes from the [PaymentProvider] and
 * the [PaymentUiMode] bound in Koin. Call it unconditionally on the screen that pays.
 */
@Composable
fun rememberCheckout(
    createIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
    onResult: (CheckoutResult) -> Unit,
): CheckoutController {
    // Previews have no Koin and no payment SDK
    if (LocalInspectionMode.current) {
        return remember { PreviewCheckoutController() }
    }

    val provider = koinInject<PaymentProvider>()
    val mode = koinInject<PaymentUiMode>()
    val currentCreateIntent = rememberUpdatedState(createIntent)
    val currentOnResult = rememberUpdatedState(onResult)
    return provider.rememberCheckoutController(
        mode = mode,
        createIntent = { save -> currentCreateIntent.value(save) },
        onResult = { result -> currentOnResult.value(result) }
    )
}

private class PreviewCheckoutController : CheckoutController {
    override val mode = PaymentUiMode.FLOW_CONTROLLER
    override val state: StateFlow<CheckoutState> = MutableStateFlow(
        CheckoutState(isReady = true, option = PaymentOption("•••• 4242"))
    )

    override suspend fun prepare(session: CheckoutSession) = true
    override fun changePaymentOption() = Unit
    override fun confirm() = Unit

    @Composable
    override fun Content(modifier: Modifier) = Unit
}
