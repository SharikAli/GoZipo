package com.mobile.booking.presentation.booking

import androidx.compose.foundation.text.input.TextFieldState
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.route.Route
import com.mobile.core.presentation.util.UiText
import com.mobile.ride.domain.FareEstimate
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.VehicleType

data class BookingState(
    val locationError: LocationError? = null,
    val userLocation: GeoPoint? = null,

    /** Center of the map while choosing pickup; fixed once a destination is set. */
    val pickup: GeoPoint? = null,
    val pickupAddress: String? = null,
    val isResolvingPickup: Boolean = false,
    val dropoff: Place? = null,
    val route: Route? = null,
    val isLoadingRoute: Boolean = false,

    val estimates: List<FareEstimate> = emptyList(),
    val isLoadingEstimates: Boolean = false,
    val estimatesFailed: Boolean = false,
    val selectedVehicle: VehicleType? = null,
    /** The customer's offer for [selectedVehicle], or the raised fare while searching. */
    val offeredFare: Int? = null,
    val autoAccept: Boolean = false,
    val isRequesting: Boolean = false,

    val search: PlaceSearch? = null,

    /** The customer's active ride (SEARCHING to IN_PROGRESS). */
    val ride: Ride? = null,
    val offers: List<RideOffer> = emptyList(),
    val acceptingOfferId: String? = null,
    val isUpdatingFare: Boolean = false,
    val isCancelling: Boolean = false,
    val riderLocation: GeoPoint? = null,
    /** A ride that just completed or was cancelled by someone else, shown until dismissed. */
    val finishedRide: Ride? = null,
) {
    val stage: BookingStage
        get() = when {
            ride != null && ride.status == RideStatus.SEARCHING -> BookingStage.SEARCHING
            ride != null -> BookingStage.TRIP
            finishedRide != null -> BookingStage.FINISHED
            dropoff != null -> BookingStage.CHOOSE_VEHICLE
            else -> BookingStage.CHOOSE_STOPS
        }

    val selectedEstimate: FareEstimate?
        get() = estimates.firstOrNull { it.vehicleType == selectedVehicle }

    /** While searching, the estimate for the ride's vehicle bounds the raised fare. */
    val rideEstimate: FareEstimate?
        get() = ride?.let { ride -> estimates.firstOrNull { it.vehicleType == ride.vehicleType } }

    /** The search round ended without an accepted offer; the customer raises the fare or retries. */
    val isSearchRoundOver: Boolean
        get() = ride?.status == RideStatus.SEARCHING && ride.searchExpiresAt == null

    /** PKR step of the fare stepper. */
    val fareStep: Int
        get() = (if (stage == BookingStage.SEARCHING) rideEstimate else selectedEstimate)?.fareStep
            ?: DEFAULT_FARE_STEP

    /**
     * Allowed values of [offeredFare]: the estimate's range while choosing, and while searching
     * from the current fare (it can only go up) to the estimate's maximum.
     */
    val fareRange: IntRange?
        get() = when (stage) {
            BookingStage.CHOOSE_VEHICLE -> selectedEstimate?.let { it.minimumFare..it.maximumFare }
            BookingStage.SEARCHING -> ride?.let { ride ->
                ride.offeredFare..maxOf(
                    ride.offeredFare,
                    rideEstimate?.maximumFare ?: ride.offeredFare
                )
            }

            else -> null
        }

    /** While searching: the fare was raised, or the round ended and the same fare can be retried. */
    val canConfirmFare: Boolean
        get() {
            val ride = ride ?: return false
            val fare = offeredFare ?: return false
            return fare > ride.offeredFare || (isSearchRoundOver && fare == ride.offeredFare)
        }

    val showUserLocation: Boolean
        get() = userLocation != null

    private companion object {
        const val DEFAULT_FARE_STEP = 10
    }
}

enum class BookingStage {
    CHOOSE_STOPS,
    CHOOSE_VEHICLE,
    SEARCHING,
    TRIP,
    FINISHED
}

enum class SearchTarget {
    PICKUP,
    DROPOFF
}

data class PlaceSearch(
    val target: SearchTarget,
    val query: TextFieldState = TextFieldState(),
    val results: List<Place> = emptyList(),
    val isSearching: Boolean = false,
    val error: UiText? = null,
)
