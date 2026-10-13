package com.mobile.booking.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.mobile.ride.domain.Vehicle
import com.mobile.ride.domain.VehicleType
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.vehicle_mini
import gozipo.feature.booking.presentation.generated.resources.vehicle_mini_description
import gozipo.feature.booking.presentation.generated.resources.vehicle_moto
import gozipo.feature.booking.presentation.generated.resources.vehicle_moto_description
import gozipo.feature.booking.presentation.generated.resources.vehicle_premium
import gozipo.feature.booking.presentation.generated.resources.vehicle_premium_description
import gozipo.feature.booking.presentation.generated.resources.vehicle_rickshaw
import gozipo.feature.booking.presentation.generated.resources.vehicle_rickshaw_description
import gozipo.feature.booking.presentation.generated.resources.vehicle_ride_ac
import gozipo.feature.booking.presentation.generated.resources.vehicle_ride_ac_description
import org.jetbrains.compose.resources.StringResource

/** How a vehicle type is shown in the "Select vehicle" list. */
data class VehicleTypeUi(
    val title: StringResource,
    val description: StringResource,
    val seats: Int,
    val icon: ImageVector
)

fun VehicleType.toUi(): VehicleTypeUi = when (this) {
    VehicleType.MOTO -> VehicleTypeUi(Res.string.vehicle_moto, Res.string.vehicle_moto_description, 1, Icons.Default.TwoWheeler)
    VehicleType.RICKSHAW -> VehicleTypeUi(Res.string.vehicle_rickshaw, Res.string.vehicle_rickshaw_description, 3, Icons.Default.ElectricRickshaw)
    VehicleType.MINI -> VehicleTypeUi(Res.string.vehicle_mini, Res.string.vehicle_mini_description, 4, Icons.Default.DirectionsCar)
    VehicleType.RIDE_AC -> VehicleTypeUi(Res.string.vehicle_ride_ac, Res.string.vehicle_ride_ac_description, 4, Icons.Default.DirectionsCar)
    VehicleType.PREMIUM -> VehicleTypeUi(Res.string.vehicle_premium, Res.string.vehicle_premium_description, 4, Icons.Default.LocalTaxi)
}

/** "Blue Suzuki Mehran". */
fun Vehicle.description(): String = listOf(color, make, model).filter { it.isNotBlank() }.joinToString(" ")
