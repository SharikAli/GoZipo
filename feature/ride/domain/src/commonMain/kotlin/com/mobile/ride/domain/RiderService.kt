package com.mobile.ride.domain

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result

/** The signed-in rider's vehicle and availability (`/api/riders/me`). */
interface RiderService {

    suspend fun getStatus(): Result<RiderStatus, DataError.Remote>

    /** Totals from the backend, using this device's time zone for "today". */
    suspend fun getEarnings(): Result<RiderEarnings, DataError.Remote>

    /** Adding or changing the vehicle takes the rider offline. Fails with CONFLICT for a taken plate. */
    suspend fun saveVehicle(vehicle: Vehicle): Result<Vehicle, DataError.Remote>

    /** Fails with FORBIDDEN when the rider has no vehicle yet. */
    suspend fun goOnline(location: GeoPoint): Result<RiderStatus, DataError.Remote>

    suspend fun goOffline(): EmptyResult<DataError.Remote>

    /**
     * Keeps the rider online and moves them on the customer's map during a ride. Goes over the
     * realtime socket, or REST when the socket isn't connected.
     */
    suspend fun shareLocation(location: GeoPoint): EmptyResult<DataError.Remote>
}
