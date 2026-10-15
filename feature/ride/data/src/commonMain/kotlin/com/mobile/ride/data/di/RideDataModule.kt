package com.mobile.ride.data.di

import com.mobile.ride.data.realtime.RealtimeRideEventSource
import com.mobile.ride.data.remote.KtorRideService
import com.mobile.ride.data.remote.KtorRiderService
import com.mobile.ride.domain.RideEventSource
import com.mobile.ride.domain.RideService
import com.mobile.ride.domain.RiderService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val rideDataModule = module {
    singleOf(::KtorRideService) bind RideService::class
    singleOf(::KtorRiderService) bind RiderService::class
    singleOf(::RealtimeRideEventSource) bind RideEventSource::class
}
