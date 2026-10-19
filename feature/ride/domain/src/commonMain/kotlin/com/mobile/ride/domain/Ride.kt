package com.mobile.ride.domain

import com.mobile.core.domain.location.GeoPoint
import kotlin.time.Duration
import kotlin.time.Instant

data class RideStop(
    val point: GeoPoint,
    val address: String
)

data class Vehicle(
    val type: VehicleType,
    val make: String,
    val model: String,
    val color: String,
    val plateNumber: String
)

data class RideCustomer(
    val id: String,
    val username: String
)

data class RideRider(
    val id: String,
    val username: String,
    val vehicle: Vehicle?
)

/**
 * A ride as the backend sees it. Fares are whole PKR.
 *
 * @param agreedFare set once a rider is assigned; the fare to pay in cash
 * @param searchExpiresAt end of the current search round, or null once it ended or a rider was assigned
 */
data class Ride(
    val id: String,
    val status: RideStatus,
    val vehicleType: VehicleType,
    val pickup: RideStop,
    val dropoff: RideStop,
    val distanceMeters: Int,
    val duration: Duration,
    val recommendedFare: Int,
    val offeredFare: Int,
    val agreedFare: Int?,
    val autoAccept: Boolean,
    val searchRound: Int,
    val searchExpiresAt: Instant?,
    val customer: RideCustomer,
    val rider: RideRider?,
    val createdAt: Instant,
    val acceptedAt: Instant?,
    val arrivedAt: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val cancelledAt: Instant?,
    val cancelledBy: CancelledBy?,
    val cancelReason: String?
)

/**
 * A rider's answer to a ride request in one search round.
 *
 * @param pickupDistanceMeters straight-line distance from the rider to the pickup, when known
 */
data class RideOffer(
    val id: String,
    val rideId: String,
    val amount: Int,
    val kind: OfferKind,
    val status: OfferStatus,
    val searchRound: Int,
    val rider: RideRider,
    val pickupDistanceMeters: Int?,
    val createdAt: Instant
)

/**
 * The backend's recommended fare and allowed offer range for one vehicle type.
 * Offers must be multiples of [fareStep] within [minimumFare]..[maximumFare].
 */
data class FareEstimate(
    val vehicleType: VehicleType,
    val recommendedFare: Int,
    val minimumFare: Int,
    val maximumFare: Int,
    val fareStep: Int,
    val distanceMeters: Int,
    val duration: Duration
)

data class RideRequest(
    val pickup: RideStop,
    val dropoff: RideStop,
    val vehicleType: VehicleType,
    val offeredFare: Int,
    val autoAccept: Boolean
)

/**
 * A request a rider can offer on: the customer's fare and how high a counter-offer may go.
 *
 * @param pickupDistanceMeters straight-line distance from the rider to the pickup, when known
 * @param myOffer this rider's offer in the current round, if any
 */
data class OpenRide(
    val rideId: String,
    val vehicleType: VehicleType,
    val pickup: RideStop,
    val dropoff: RideStop,
    val distanceMeters: Int,
    val duration: Duration,
    val offeredFare: Int,
    val maximumCounterFare: Int,
    val fareStep: Int,
    val pickupDistanceMeters: Int?,
    val searchRound: Int,
    val searchExpiresAt: Instant?,
    val customer: RideCustomer,
    val myOffer: RideOffer?
)

/** The signed-in rider's availability; [vehicle] is null until the rider adds one. */
data class RiderStatus(
    val online: Boolean,
    val vehicle: Vehicle?
)

/** Completed trips and their cash fares (PKR) in one period, summed by the backend. */
data class EarningsPeriod(
    val trips: Int,
    val amount: Int
)

/** The rider's earnings by their local day: today and the last 7 days including today. */
data class RiderEarnings(
    val today: EarningsPeriod,
    val last7Days: EarningsPeriod
)
