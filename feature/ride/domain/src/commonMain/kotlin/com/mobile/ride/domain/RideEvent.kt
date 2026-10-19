package com.mobile.ride.domain

import com.mobile.core.domain.location.GeoPoint

/** A ride change pushed over the realtime socket. Missed events are recovered by resyncing over REST. */
sealed interface RideEvent {
    /** The ride changed (status, fare, rider assigned, canceled, ...). */
    data class RideUpdated(val ride: Ride) : RideEvent

    /** Customer: the search round ended without an accepted offer. Raise the fare or cancel. */
    data class SearchRoundEnded(val ride: Ride) : RideEvent

    /** Customer: a rider offered on the ride. */
    data class OfferReceived(val offer: RideOffer) : RideEvent

    /** An offer's status changed (withdrawn, declined, expired, accepted). */
    data class OfferUpdated(val offer: RideOffer) : RideEvent

    /** Customer: where the assigned rider is now. */
    data class RiderLocation(val rideId: String, val point: GeoPoint) : RideEvent

    /** Rider: a new request nearby, or one they saw was re-priced (new search round). */
    data class RideRequested(val ride: OpenRide) : RideEvent

    /** Rider: a request they saw is no longer open. */
    data class RideUnavailable(val rideId: String) : RideEvent
}
