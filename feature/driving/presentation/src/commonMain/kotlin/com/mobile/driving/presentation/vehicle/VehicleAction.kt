package com.mobile.driving.presentation.vehicle

import com.mobile.ride.domain.VehicleType

sealed interface VehicleAction {
    data class OnVehicleTypeClick(val vehicleType: VehicleType) : VehicleAction
    data object OnSaveClick : VehicleAction
    data object OnBackClick : VehicleAction
}
