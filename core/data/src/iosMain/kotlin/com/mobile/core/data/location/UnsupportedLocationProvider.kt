package com.mobile.core.data.location

import com.mobile.core.domain.location.DeviceLocation
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** iOS location (Core Location) is not implemented yet; see Phase 5 in document.md. */
class UnsupportedLocationProvider : LocationProvider {
    override fun checkAvailability(): EmptyResult<LocationError> =
        Result.Failure(LocationError.UNSUPPORTED)

    override suspend fun getCurrentLocation(): Result<DeviceLocation, LocationError> =
        Result.Failure(LocationError.UNSUPPORTED)

    override fun observeLocation(intervalMillis: Long): Flow<DeviceLocation> = emptyFlow()
}
