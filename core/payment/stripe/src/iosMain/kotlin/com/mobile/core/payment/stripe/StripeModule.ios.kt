package com.mobile.core.payment.stripe

import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.payment.api.CheckoutController
import com.mobile.core.payment.api.CheckoutResult
import com.mobile.core.payment.api.CheckoutSession
import com.mobile.core.payment.api.CheckoutState
import com.mobile.core.payment.api.IntentCreation
import com.mobile.core.payment.api.PaymentProvider
import com.mobile.core.payment.api.PaymentUiMode
import com.mobile.core.payment.stripe.ios.GoZipoStripeFactory
import com.mobile.core.payment.stripe.ios.StripeIosPaymentProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformStripeModule = module {
    single<PaymentProvider> {
        getOrNull<GoZipoStripeFactory>()?.let(::StripeIosPaymentProvider) ?: run {
            get<GoZipoLogger>().error("No GoZipoStripeFactory registered; start Koin with doInitKoin(mapboxFactory:stripeFactory:)")
            UnavailablePaymentProvider
        }
    }
}

/** Registers the Swift side of the Stripe bridge. The customer iOS app's `initKoin` adds this. */
fun stripeIosBridgeModule(factory: GoZipoStripeFactory): Module = module {
    single<GoZipoStripeFactory> { factory }
}

/** Without the Swift bridge: preparing fails with a message instead of crashing. */
private object UnavailablePaymentProvider : PaymentProvider {
    @Composable
    override fun rememberCheckoutController(
        mode: PaymentUiMode,
        createIntent: suspend (savePaymentMethod: Boolean) -> IntentCreation,
        onResult: (CheckoutResult) -> Unit,
    ): CheckoutController = remember {
        object : CheckoutController {
            override val mode = mode
            override val state: StateFlow<CheckoutState> = MutableStateFlow(
                CheckoutState(errorMessage = "Card payments are unavailable")
            )

            override suspend fun prepare(session: CheckoutSession) = false
            override fun changePaymentOption() = Unit
            override fun confirm() = Unit

            @Composable
            override fun Content(modifier: Modifier) = Unit
        }
    }
}
