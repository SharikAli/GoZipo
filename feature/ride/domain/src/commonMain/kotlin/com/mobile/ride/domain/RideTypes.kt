package com.mobile.ride.domain

/** Passenger vehicle classes. Fares and rules per type come from the backend. */
enum class VehicleType {
    MOTO,
    RICKSHAW,
    MINI,
    RIDE_AC,
    PREMIUM
}

/**
 * Server-owned ride lifecycle:
 * SEARCHING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED, with CANCELLED until the trip starts.
 * The app only displays it; the backend decides every transition.
 */
enum class RideStatus {
    SEARCHING,
    ACCEPTED,
    ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    val isActive: Boolean
        get() = this != COMPLETED && this != CANCELLED
}

enum class OfferStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    WITHDRAWN
}

enum class OfferKind {
    /** The rider takes the customer's fare as-is. */
    ACCEPT_FARE,

    /** The rider asks for more than the customer's fare. */
    COUNTER
}

enum class CancelledBy {
    CUSTOMER,
    RIDER,
    SYSTEM
}
