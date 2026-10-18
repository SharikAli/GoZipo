package com.mobile.payment.data.di

import com.mobile.payment.data.realtime.RealtimePaymentEventSource
import com.mobile.payment.data.remote.KtorPaymentService
import com.mobile.payment.domain.PaymentEventSource
import com.mobile.payment.domain.PaymentService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val paymentDataModule = module {
    singleOf(::KtorPaymentService) bind PaymentService::class
    singleOf(::RealtimePaymentEventSource) bind PaymentEventSource::class
}
