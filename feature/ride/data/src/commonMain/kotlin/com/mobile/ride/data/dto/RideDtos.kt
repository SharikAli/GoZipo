package com.mobile.ride.data.dto

import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.OfferKind
import com.mobile.ride.domain.OfferStatus
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.VehicleType
import kotlinx.serialization.Serializable

// Mirrors the backend's ride DTOs. Instants arrive as ISO-8601 strings.

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class StopDto(
    val location: LocationDto,
    val address: String
)

@Serializable
data class FareEstimateRequestDto(
    val pickup: LocationDto,
    val dropoff: LocationDto
)

@Serializable
data class CreateRideRequestDto(
    val pickup: StopDto,
    val dropoff: StopDto,
    val vehicleType: VehicleType,
    val offeredFare: Int,
    val autoAccept: Boolean
)

@Serializable
data class UpdateFareRequestDto(
    val offeredFare: Int
)

@Serializable
data class CancelRideRequestDto(
    val reason: String? = null
)

@Serializable
data class FareEstimateDto(
    val vehicleType: VehicleType,
    val recommendedFare: Int,
    val minimumFare: Int,
    val maximumFare: Int,
    val fareStep: Int,
    val distanceMeters: Int,
    val durationSeconds: Int
)

@Serializable
data class VehicleDto(
    val vehicleType: VehicleType,
    val make: String,
    val model: String,
    val color: String,
    val plateNumber: String
)

@Serializable
data class RideCustomerDto(
    val id: String,
    val username: String
)

@Serializable
data class RideRiderDto(
    val id: String,
    val username: String,
    val vehicle: VehicleDto? = null
)

@Serializable
data class RideDto(
    val id: String,
    val status: RideStatus,
    val vehicleType: VehicleType,
    val pickup: StopDto,
    val dropoff: StopDto,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val recommendedFare: Int,
    val offeredFare: Int,
    val agreedFare: Int? = null,
    val autoAccept: Boolean,
    val searchRound: Int,
    val searchExpiresAt: String? = null,
    val customer: RideCustomerDto,
    val rider: RideRiderDto? = null,
    val createdAt: String,
    val acceptedAt: String? = null,
    val arrivedAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val cancelledBy: CancelledBy? = null,
    val cancelReason: String? = null
)

@Serializable
data class OfferDto(
    val id: String,
    val rideId: String,
    val amount: Int,
    val kind: OfferKind,
    val status: OfferStatus,
    val searchRound: Int,
    val rider: RideRiderDto,
    val pickupDistanceMeters: Int? = null,
    val createdAt: String
)

@Serializable
data class RiderLocationDto(
    val rideId: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class RideRefDto(
    val rideId: String
)

/** Empty body for endpoints that take none. */
@Serializable
class EmptyRequestDto
