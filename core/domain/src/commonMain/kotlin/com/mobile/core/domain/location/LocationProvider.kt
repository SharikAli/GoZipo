package com.mobile.core.domain.location

import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * The device's own position. Independent of the map provider, so swapping the map
 * (Mapbox, Google Maps, ...) never touches location code.
 *
 * Permission requests are a UI concern (`PermissionController` in core/presentation);
 * this only reports whether location can be read.
 */
interface LocationProvider {
    /** Success when location can be read now, otherwise the reason it can't. */
    fun checkAvailability(): EmptyResult<LocationError>

    /** One fresh fix, falling back to the last known one. */
    suspend fun getCurrentLocation(): Result<DeviceLocation, LocationError>

    /**
     * Continuous updates while collected. Emits nothing when location isn't available;
     * call [checkAvailability] first.
     */
    fun observeLocation(intervalMillis: Long = DEFAULT_INTERVAL_MILLIS): Flow<DeviceLocation>

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 5_000L
    }
}
