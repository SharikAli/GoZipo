package com.mobile.ride.data.mappers

import com.mobile.core.domain.location.GeoPoint
import com.mobile.ride.data.dto.EarningsDto
import com.mobile.ride.data.dto.EarningsPeriodDto
import com.mobile.ride.data.dto.FareEstimateDto
import com.mobile.ride.data.dto.LocationDto
import com.mobile.ride.data.dto.OfferDto
import com.mobile.ride.data.dto.OpenRideDto
import com.mobile.ride.data.dto.RideCustomerDto
import com.mobile.ride.data.dto.RideDto
import com.mobile.ride.data.dto.RideRiderDto
import com.mobile.ride.data.dto.RiderStatusDto
import com.mobile.ride.data.dto.StopDto
import com.mobile.ride.data.dto.VehicleDto
import com.mobile.ride.domain.EarningsPeriod
import com.mobile.ride.domain.FareEstimate
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideCustomer
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideRider
import com.mobile.ride.domain.RiderEarnings
import com.mobile.ride.domain.RiderStatus
import com.mobile.ride.domain.RideStop
import com.mobile.ride.domain.Vehicle
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun GeoPoint.toDto() = LocationDto(latitude = latitude, longitude = longitude)

fun LocationDto.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

fun RideStop.toDto() = StopDto(location = point.toDto(), address = address)

fun StopDto.toRideStop() = RideStop(point = location.toGeoPoint(), address = address)

fun FareEstimateDto.toFareEstimate() = FareEstimate(
    vehicleType = vehicleType,
    recommendedFare = recommendedFare,
    minimumFare = minimumFare,
    maximumFare = maximumFare,
    fareStep = fareStep,
    distanceMeters = distanceMeters,
    duration = durationSeconds.seconds
)

fun VehicleDto.toVehicle() = Vehicle(
    type = vehicleType,
    make = make,
    model = model,
    color = color,
    plateNumber = plateNumber
)

fun Vehicle.toDto() = VehicleDto(
    vehicleType = type,
    make = make,
    model = model,
    color = color,
    plateNumber = plateNumber
)

fun RiderStatusDto.toRiderStatus() = RiderStatus(online = online, vehicle = vehicle?.toVehicle())

fun EarningsPeriodDto.toEarningsPeriod() = EarningsPeriod(trips = trips, amount = amount)

fun EarningsDto.toRiderEarnings() = RiderEarnings(
    today = today.toEarningsPeriod(),
    last7Days = last7Days.toEarningsPeriod()
)

fun RideCustomerDto.toRideCustomer() = RideCustomer(id = id, username = username)

fun RideRiderDto.toRideRider() =
    RideRider(id = id, username = username, vehicle = vehicle?.toVehicle())

fun RideDto.toRide() = Ride(
    id = id,
    status = status,
    vehicleType = vehicleType,
    pickup = pickup.toRideStop(),
    dropoff = dropoff.toRideStop(),
    distanceMeters = distanceMeters,
    duration = durationSeconds.seconds,
    recommendedFare = recommendedFare,
    offeredFare = offeredFare,
    agreedFare = agreedFare,
    autoAccept = autoAccept,
    searchRound = searchRound,
    searchExpiresAt = searchExpiresAt?.toInstant(),
    customer = customer.toRideCustomer(),
    rider = rider?.toRideRider(),
    createdAt = createdAt.toInstant(),
    acceptedAt = acceptedAt?.toInstant(),
    arrivedAt = arrivedAt?.toInstant(),
    startedAt = startedAt?.toInstant(),
    completedAt = completedAt?.toInstant(),
    cancelledAt = cancelledAt?.toInstant(),
    cancelledBy = cancelledBy,
    cancelReason = cancelReason
)

fun OfferDto.toRideOffer() = RideOffer(
    id = id,
    rideId = rideId,
    amount = amount,
    kind = kind,
    status = status,
    searchRound = searchRound,
    rider = rider.toRideRider(),
    pickupDistanceMeters = pickupDistanceMeters,
    createdAt = createdAt.toInstant()
)

fun OpenRideDto.toOpenRide() = OpenRide(
    rideId = rideId,
    vehicleType = vehicleType,
    pickup = pickup.toRideStop(),
    dropoff = dropoff.toRideStop(),
    distanceMeters = distanceMeters,
    duration = durationSeconds.seconds,
    offeredFare = offeredFare,
    maximumCounterFare = maximumCounterFare,
    fareStep = fareStep,
    pickupDistanceMeters = pickupDistanceMeters,
    searchRound = searchRound,
    searchExpiresAt = searchExpiresAt?.toInstant(),
    customer = customer.toRideCustomer(),
    myOffer = myOffer?.toRideOffer()
)

private fun String.toInstant(): Instant = Instant.parse(this)
