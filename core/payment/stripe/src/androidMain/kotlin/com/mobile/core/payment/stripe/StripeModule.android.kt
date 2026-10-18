package com.mobile.core.payment.stripe

import com.mobile.core.payment.api.PaymentProvider
import org.koin.dsl.module

internal actual val platformStripeModule = module {
    single<PaymentProvider> { StripeAndroidPaymentProvider() }
}
