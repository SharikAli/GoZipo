package com.mobile.booking.presentation.booking

import androidx.compose.foundation.text.input.TextFieldState
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.route.Route
import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.FareEstimate
import com.mobile.ride.domain.OfferKind
import com.mobile.ride.domain.OfferStatus
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

/** Sample [BookingState]s for previews, one per stage. */
internal object BookingPreviewData {

    private val pickupPoint = GeoPoint(24.9425, 67.114)
    private val dropoffPoint = GeoPoint(24.93, 67.07)

    private val dropoff = Place(
        id = "1",
        name = "Aisha Manzil mosque",
        address = "Block 7, Gulberg",
        point = dropoffPoint
    )

    private val route = Route(
        distanceMeters = 9_800,
        duration = 22.minutes,
        points = listOf(pickupPoint, GeoPoint(24.938, 67.095), dropoffPoint)
    )

    private val estimates = listOf(
        FareEstimate(VehicleType.MINI, 540, 380, 1620, 10, 9_800, 22.minutes),
        FareEstimate(VehicleType.MOTO, 240, 170, 720, 10, 9_800, 18.minutes),
        FareEstimate(VehicleType.RICKSHAW, 470, 330, 1410, 10, 9_800, 24.minutes),
        FareEstimate(VehicleType.RIDE_AC, 720, 500, 2160, 10, 9_800, 22.minutes),
    )

    private val rider = RideRider(
        id = "rider-1",
        username = "Imran",
        vehicle = Vehicle(VehicleType.MINI, "Suzuki", "Alto", "White", "LEA-1234")
    )

    private fun ride(
        status: RideStatus,
        agreedFare: Int? = null,
        searchExpiresIn: Boolean = false,
        cancelledBy: CancelledBy? = null,
    ): Ride {
        val now = Clock.System.now()
        return Ride(
            id = "ride-1",
            status = status,
            vehicleType = VehicleType.MINI,
            pickup = RideStop(pickupPoint, "Dr Abdus Salam Rd"),
            dropoff = RideStop(dropoffPoint, "Aisha Manzil mosque"),
            distanceMeters = 9_800,
            duration = 22.minutes,
            recommendedFare = 540,
            offeredFare = 540,
            agreedFare = agreedFare,
            autoAccept = false,
            searchRound = 1,
            searchExpiresAt = if (searchExpiresIn) now + 42.seconds else null,
            customer = RideCustomer("customer-1", "Sara"),
            rider = rider.takeIf { status != RideStatus.SEARCHING && cancelledBy != CancelledBy.SYSTEM },
            createdAt = now,
            acceptedAt = null,
            arrivedAt = null,
            startedAt = null,
            completedAt = null,
            cancelledAt = null,
            cancelledBy = cancelledBy,
            cancelReason = null
        )
    }

    private fun offer(id: String, amount: Int, kind: OfferKind, username: String, distance: Int) = RideOffer(
        id = id,
        rideId = "ride-1",
        amount = amount,
        kind = kind,
        status = OfferStatus.PENDING,
        searchRound = 1,
        rider = rider.copy(id = "rider-$id", username = username),
        pickupDistanceMeters = distance,
        createdAt = Clock.System.now()
    )

    val chooseStops = BookingState(
        userLocation = pickupPoint,
        pickup = pickupPoint,
        pickupAddress = "Dr Abdus Salam Rd",
    )

    val chooseStopsResolving = chooseStops.copy(
        pickupAddress = null,
        isResolvingPickup = true,
    )

    val locationDenied = BookingState(
        locationError = LocationError.PERMISSION_DENIED,
    )

    val placeSearch = chooseStops.copy(
        search = PlaceSearch(
            target = SearchTarget.DROPOFF,
            query = TextFieldState("Aisha"),
            results = listOf(
                dropoff,
                Place("2", "Aisha Bawany College", "Shahrah-e-Faisal", GeoPoint(24.86, 67.08)),
                Place("3", "Aisha Medical Centre", "North Nazimabad", GeoPoint(24.94, 67.04)),
            )
        )
    )

    val destinationOnMap = chooseStops.copy(
        destinationPin = DestinationPin(point = dropoffPoint, address = "Aisha Manzil mosque, Block 7, Gulberg")
    )

    val placeSearchEmpty = chooseStops.copy(
        search = PlaceSearch(target = SearchTarget.PICKUP, query = TextFieldState("zzqx"))
    )

    val chooseVehicleLoading = chooseStops.copy(
        dropoff = dropoff,
        isLoadingRoute = true,
        isLoadingEstimates = true,
    )

    val chooseVehicle = chooseStops.copy(
        dropoff = dropoff,
        route = route,
        estimates = estimates,
        selectedVehicle = VehicleType.MINI,
        offeredFare = 540,
    )

    val chooseVehicleFailed = chooseStops.copy(
        dropoff = dropoff,
        route = route,
        estimatesFailed = true,
    )

    val searching = chooseVehicle.copy(
        ride = ride(RideStatus.SEARCHING, searchExpiresIn = true),
        offeredFare = 540,
    )

    val searchingWithOffers = searching.copy(
        offers = listOf(
            offer("o1", 540, OfferKind.ACCEPT_FARE, "Imran", 650),
            offer("o2", 620, OfferKind.COUNTER, "Bilal", 1_400),
        )
    )

    val searchRoundOver = searching.copy(
        ride = ride(RideStatus.SEARCHING),
        offeredFare = 580,
    )

    val driverArriving = chooseVehicle.copy(
        ride = ride(RideStatus.ACCEPTED, agreedFare = 540),
        riderLocation = GeoPoint(24.947, 67.121),
    )

    val driverArrived = driverArriving.copy(
        ride = ride(RideStatus.ARRIVED, agreedFare = 540),
        riderLocation = pickupPoint,
    )

    val inProgress = driverArriving.copy(
        ride = ride(RideStatus.IN_PROGRESS, agreedFare = 540),
        riderLocation = GeoPoint(24.938, 67.095),
    )

    val completed = chooseVehicle.copy(
        finishedRide = ride(RideStatus.COMPLETED, agreedFare = 540),
    )

    val cancelledByRider = chooseVehicle.copy(
        finishedRide = ride(RideStatus.CANCELLED, cancelledBy = CancelledBy.RIDER),
    )
}
