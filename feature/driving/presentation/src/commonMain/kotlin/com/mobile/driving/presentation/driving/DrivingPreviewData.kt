package com.mobile.driving.presentation.driving

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.route.Route
import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.OfferKind
import com.mobile.ride.domain.OfferStatus
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideCustomer
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideRider
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.RideStop
import com.mobile.ride.domain.Vehicle
import com.mobile.ride.domain.VehicleType
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Sample [DrivingState]s for previews, one per stage. */
internal object DrivingPreviewData {

    private val riderPoint = GeoPoint(24.947, 67.121)
    private val pickupPoint = GeoPoint(24.9425, 67.114)
    private val dropoffPoint = GeoPoint(24.93, 67.07)

    private val vehicle = Vehicle(VehicleType.MINI, "Suzuki", "Alto", "White", "LEA-1234")
    private val customer = RideCustomer("customer-1", "Sara")
    private val pickup = RideStop(pickupPoint, "Dr Abdus Salam Rd")
    private val dropoff = RideStop(dropoffPoint, "Aisha Manzil mosque")

    private fun request(
        id: String,
        fare: Int,
        pickupDistance: Int,
        customerName: String,
        pickupAddress: String,
        dropoffAddress: String,
    ) = OpenRide(
        rideId = id,
        vehicleType = VehicleType.MINI,
        pickup = RideStop(pickupPoint, pickupAddress),
        dropoff = RideStop(dropoffPoint, dropoffAddress),
        distanceMeters = 9_800,
        duration = 22.minutes,
        offeredFare = fare,
        maximumCounterFare = fare * 3 / 2 / 10 * 10,
        fareStep = 10,
        pickupDistanceMeters = pickupDistance,
        searchRound = 1,
        searchExpiresAt = Clock.System.now() + 42.seconds,
        customer = RideCustomer("customer-$id", customerName),
        myOffer = null
    )

    val requests = listOf(
        request("ride-1", 540, 650, "Sara", "Dr Abdus Salam Rd", "Aisha Manzil mosque"),
        request("ride-2", 380, 1_900, "Ahmed", "Gulshan Chowrangi", "Lucky One Mall"),
    )

    private fun ride(status: RideStatus, cancelledBy: CancelledBy? = null): Ride {
        val now = Clock.System.now()
        return Ride(
            id = "ride-1",
            status = status,
            vehicleType = VehicleType.MINI,
            pickup = pickup,
            dropoff = dropoff,
            distanceMeters = 9_800,
            duration = 22.minutes,
            recommendedFare = 540,
            offeredFare = 540,
            agreedFare = 600,
            autoAccept = false,
            searchRound = 1,
            searchExpiresAt = null,
            customer = customer,
            rider = RideRider("rider-1", "Imran", vehicle),
            createdAt = now,
            acceptedAt = now,
            arrivedAt = null,
            startedAt = null,
            completedAt = null,
            cancelledAt = null,
            cancelledBy = cancelledBy,
            cancelReason = null
        )
    }

    val loading = DrivingState()

    val noVehicle = DrivingState(isStatusLoading = false)

    val offline = DrivingState(
        isStatusLoading = false,
        userLocation = riderPoint,
        vehicle = vehicle,
    )

    val offlineLocationDenied = offline.copy(
        userLocation = null,
        locationError = LocationError.PERMISSION_DENIED,
    )

    val onlineWaiting = offline.copy(isOnline = true)

    val onlineRequests = onlineWaiting.copy(requests = requests)

    val onlineCounter = onlineRequests.copy(
        selectedRideId = "ride-1",
        offerAmount = 600,
    )

    val onlineOfferSent = onlineRequests.copy(
        requests = listOf(
            requests[0].copy(
                myOffer = RideOffer(
                    id = "offer-1",
                    rideId = "ride-1",
                    amount = 600,
                    kind = OfferKind.COUNTER,
                    status = OfferStatus.PENDING,
                    searchRound = 1,
                    rider = RideRider("rider-1", "Imran", vehicle),
                    pickupDistanceMeters = 650,
                    createdAt = Clock.System.now()
                )
            ),
            requests[1]
        )
    )

    private val routeToPickup = Route(
        distanceMeters = 650,
        duration = 3.minutes,
        points = listOf(riderPoint, GeoPoint(24.945, 67.118), pickupPoint)
    )

    val toPickup = onlineWaiting.copy(
        ride = ride(RideStatus.ACCEPTED),
        route = routeToPickup,
    )

    val atPickup = toPickup.copy(
        ride = ride(RideStatus.ARRIVED),
        userLocation = pickupPoint,
    )

    val inProgress = toPickup.copy(
        ride = ride(RideStatus.IN_PROGRESS),
        route = Route(
            9_800,
            22.minutes,
            listOf(pickupPoint, GeoPoint(24.938, 67.095), dropoffPoint)
        ),
        userLocation = GeoPoint(24.938, 67.095),
    )

    val completed = onlineWaiting.copy(finishedRide = ride(RideStatus.COMPLETED))

    val cancelledByCustomer = onlineWaiting.copy(
        finishedRide = ride(RideStatus.CANCELLED, cancelledBy = CancelledBy.CUSTOMER),
    )
}
