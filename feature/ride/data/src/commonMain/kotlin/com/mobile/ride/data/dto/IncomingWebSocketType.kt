package com.mobile.ride.data.dto

enum class IncomingWebSocketType {
    RIDE_UPDATED,
    SEARCH_ROUND_ENDED,
    OFFER_RECEIVED,
    OFFER_UPDATED,
    RIDER_LOCATION,
    RIDE_REQUESTED,
    RIDE_UNAVAILABLE
}

/** Messages the app sends on the ride socket. */
enum class OutgoingWebSocketType {
    /** Rider's current location ([LocationDto]). */
    LOCATION_UPDATE
}
