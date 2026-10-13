package com.mobile.booking.presentation.di

import com.mobile.booking.presentation.booking.BookingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookingPresentationModule = module {
    viewModelOf(::BookingViewModel)
}
