package com.mobile.gozipo.di

import com.mobile.apps.common.di.appCommonModule
import com.mobile.auth.data.di.authDataModule
import com.mobile.auth.presentation.di.authPresentationModule
import com.mobile.booking.presentation.di.bookingPresentationModule
import com.mobile.core.data.di.coreDataModule
import com.mobile.core.map.mapbox.mapboxModule
import com.mobile.core.domain.auth.UserRole
import com.mobile.core.payment.stripe.stripePaymentModule
import com.mobile.payment.data.di.paymentDataModule
import com.mobile.payment.presentation.di.paymentPresentationModule
import com.mobile.ride.data.di.rideDataModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appCommonModule(UserRole.CUSTOMER),
            coreDataModule,
            mapboxModule,
            authDataModule,
            authPresentationModule,
            rideDataModule,
            bookingPresentationModule,
            stripePaymentModule,
            paymentDataModule,
            paymentPresentationModule
        )
    }
}