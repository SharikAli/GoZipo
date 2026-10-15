package com.mobile.ride.domain

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result

/** Ride REST API (`/api/rides`). The backend owns state, fares and ownership. */
interface RideService {

    /** Customer: recommended fare and allowed range for every vehicle type. */
    suspend fun estimateFares(
        pickup: GeoPoint,
        dropoff: GeoPoint
    ): Result<List<FareEstimate>, DataError.Remote>

    /** Customer: start searching. Fails with CONFLICT when the customer already has an active ride. */
    suspend fun requestRide(request: RideRequest): Result<Ride, DataError.Remote>

    /** Customer: raise the fare; starts a new search round. */
    suspend fun updateFare(rideId: String, offeredFare: Int): Result<Ride, DataError.Remote>

    /** Customer: pending offers in the current round. */
    suspend fun getPendingOffers(rideId: String): Result<List<RideOffer>, DataError.Remote>

    suspend fun acceptOffer(rideId: String, offerId: String): Result<Ride, DataError.Remote>

    suspend fun declineOffer(rideId: String, offerId: String): EmptyResult<DataError.Remote>

    /** Rider: open requests nearby for the rider's vehicle. Fails with CONFLICT while offline. */
    suspend fun getOpenRides(): Result<List<OpenRide>, DataError.Remote>

    /**
     * Rider: take the customer's fare ([amount] null) or counter-offer. With auto-accept on,
     * taking the fare assigns the rider at once (the returned offer is ACCEPTED).
     */
    suspend fun submitOffer(
        rideId: String,
        amount: Int? = null
    ): Result<RideOffer, DataError.Remote>

    suspend fun withdrawOffer(rideId: String, offerId: String): EmptyResult<DataError.Remote>

    /** Rider: trip steps. The backend rejects steps out of order with CONFLICT. */
    suspend fun markArrived(rideId: String): Result<Ride, DataError.Remote>

    suspend fun startTrip(rideId: String): Result<Ride, DataError.Remote>

    suspend fun completeTrip(rideId: String): Result<Ride, DataError.Remote>

    /** Customer until the trip starts, rider once assigned. */
    suspend fun cancelRide(rideId: String, reason: String? = null): Result<Ride, DataError.Remote>

    /** The signed-in user's ride in progress, or null. Used to resync after reconnecting or restarting. */
    suspend fun getActiveRide(): Result<Ride?, DataError.Remote>

    suspend fun getRide(rideId: String): Result<Ride, DataError.Remote>

    /** Finished and cancelled rides, newest first. */
    suspend fun getHistory(page: Int): Result<List<Ride>, DataError.Remote>
}
