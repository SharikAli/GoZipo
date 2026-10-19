package com.mobile.driving.presentation.driving

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.route.Route
import com.mobile.driving.presentation.navigation.NavigationTarget
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.Vehicle

data class DrivingState(
    val locationError: LocationError? = null,
    val userLocation: GeoPoint? = null,
    val userBearing: Float? = null,

    val isStatusLoading: Boolean = true,
    val statusFailed: Boolean = false,
    val vehicle: Vehicle? = null,
    val isOnline: Boolean = false,
    val isChangingAvailability: Boolean = false,

    /** Open requests near the rider, nearest pickup first. */
    val requests: List<OpenRide> = emptyList(),
    val selectedRideId: String? = null,
    /** The amount for the selected request: its fare to take it as-is, higher to counter. */
    val offerAmount: Int? = null,
    val submittingRideId: String? = null,
    val withdrawingOfferId: String? = null,

    /** The ride this rider is assigned to (ACCEPTED to IN_PROGRESS). */
    val ride: Ride? = null,
    /** To the pickup before the trip starts, then to the destination. */
    val route: Route? = null,
    val isUpdatingTrip: Boolean = false,
    val isCancelling: Boolean = false,
    /** A ride that just completed or was canceled, shown until dismissed. */
    val finishedRide: Ride? = null,
) {
    val stage: DrivingStage
        get() = when {
            ride != null -> DrivingStage.TRIP
            finishedRide != null -> DrivingStage.FINISHED
            isStatusLoading && vehicle == null -> DrivingStage.LOADING
            vehicle == null -> DrivingStage.NO_VEHICLE
            isOnline -> DrivingStage.ONLINE
            else -> DrivingStage.OFFLINE
        }

    val selectedRequest: OpenRide?
        get() = requests.firstOrNull { it.rideId == selectedRideId }

    /** Allowed offers for the selected request: its fare up to the backend's counter limit. */
    val offerRange: IntRange?
        get() = selectedRequest?.let {
            it.offeredFare..maxOf(
                it.offeredFare,
                it.maximumCounterFare
            )
        }

    /** Location is shared while online (to get requests) and during a ride (for the customer's map). */
    val isSharingLocation: Boolean
        get() = isOnline || ride != null

    /** Guidance for the current leg; none while waiting at the pickup. */
    val navigationTarget: NavigationTarget?
        get() {
            val ride = ride ?: return null
            val stop = when (ride.status) {
                RideStatus.ACCEPTED -> ride.pickup
                RideStatus.IN_PROGRESS -> ride.dropoff
                else -> return null
            }
            return NavigationTarget(
                rideId = ride.id,
                destination = stop.point,
                destinationName = stop.address,
                origin = userLocation
            )
        }

    /** The next stop to drive to. */
    val nextStop: GeoPoint?
        get() = when (ride?.status) {
            RideStatus.ACCEPTED, RideStatus.ARRIVED -> ride.pickup.point
            RideStatus.IN_PROGRESS -> ride.dropoff.point
            else -> null
        }
}

enum class DrivingStage {
    LOADING,
    NO_VEHICLE,
    OFFLINE,
    ONLINE,
    TRIP,
    FINISHED
}
