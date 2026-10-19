package com.mobile.driving.presentation.di

import com.mobile.driving.presentation.driving.DrivingViewModel
import com.mobile.driving.presentation.history.RideHistoryViewModel
import com.mobile.driving.presentation.navigation.TurnByTurnViewModel
import com.mobile.driving.presentation.vehicle.VehicleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val drivingPresentationModule = module {
    viewModelOf(::DrivingViewModel)
    viewModelOf(::VehicleViewModel)
    viewModelOf(::RideHistoryViewModel)
    viewModel { (rideId: String) -> TurnByTurnViewModel(rideId, get(), get()) }
}
