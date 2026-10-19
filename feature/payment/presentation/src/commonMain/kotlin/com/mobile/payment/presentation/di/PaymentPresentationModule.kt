package com.mobile.payment.presentation.di

import com.mobile.payment.presentation.methods.PaymentMethodsViewModel
import com.mobile.payment.presentation.ride.RidePaymentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val paymentPresentationModule = module {
    viewModelOf(::PaymentMethodsViewModel)
    viewModel { (rideId: String, fare: Int) ->
        RidePaymentViewModel(
            rideId = rideId,
            fare = fare,
            paymentService = get(),
            paymentEventSource = get()
        )
    }
}
