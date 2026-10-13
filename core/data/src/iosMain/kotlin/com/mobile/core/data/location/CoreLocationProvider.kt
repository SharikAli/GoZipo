package com.mobile.core.data.location

import com.mobile.core.domain.location.DeviceLocation
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * [LocationProvider] on Core Location. Permission is requested by the presentation layer
 * (`PermissionController`); this only reads location once it's allowed.
 *
 * Each request uses its own `CLLocationManager`, created on the main thread so its delegate
 * callbacks arrive there.
 */
class CoreLocationProvider : LocationProvider {

    private companion object {
        val CURRENT_LOCATION_TIMEOUT = 10.seconds
    }

    override fun checkAvailability(): EmptyResult<LocationError> {
        if (!CLLocationManager.locationServicesEnabled()) {
            return Result.Failure(LocationError.SERVICES_DISABLED)
        }
        return when (CLLocationManager().authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> Result.Success(Unit)

            else -> Result.Failure(LocationError.PERMISSION_DENIED)
        }
    }

    override suspend fun getCurrentLocation(): Result<DeviceLocation, LocationError> {
        val availability = checkAvailability()
        if (availability is Result.Failure) {
            return availability
        }

        return withContext(Dispatchers.Main) {
            val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyBest }
            val location = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT) {
                manager.requestSingleLocation()
            } ?: manager.location

            if (location != null) {
                Result.Success(location.toDeviceLocation())
            } else {
                Result.Failure(LocationError.UNAVAILABLE)
            }
        }
    }

    override fun observeLocation(intervalMillis: Long): Flow<DeviceLocation> {
        if (checkAvailability() is Result.Failure) {
            return emptyFlow()
        }

        // Core Location has no update interval; drop fixes that arrive sooner than asked
        val minGap = (intervalMillis / 2).milliseconds
        return callbackFlow {
            var lastSent: TimeSource.Monotonic.ValueTimeMark? = null
            val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyBest }
            val delegate = LocationDelegate(
                onLocation = { location ->
                    val elapsed = lastSent?.elapsedNow()
                    if (elapsed == null || elapsed >= minGap) {
                        lastSent = TimeSource.Monotonic.markNow()
                        trySend(location.toDeviceLocation())
                    }
                },
                onError = {}
            )
            manager.delegate = delegate
            manager.startUpdatingLocation()

            awaitClose {
                manager.stopUpdatingLocation()
                // The manager holds its delegate weakly; this reference keeps it alive until here
                delegate.release()
                manager.delegate = null
            }
        }.flowOn(Dispatchers.Main)
    }

    private suspend fun CLLocationManager.requestSingleLocation(): CLLocation? =
        suspendCancellableCoroutine { continuation ->
            val delegate = LocationDelegate(
                onLocation = { if (continuation.isActive) continuation.resume(it) },
                onError = { if (continuation.isActive) continuation.resume(null) }
            )
            this.delegate = delegate
            requestLocation()
            continuation.invokeOnCancellation {
                delegate.release()
            }
        }
}

private class LocationDelegate(
    private var onLocation: ((CLLocation) -> Unit)?,
    private var onError: ((NSError) -> Unit)?
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let { onLocation?.invoke(it) }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError?.invoke(didFailWithError)
    }

    fun release() {
        onLocation = null
        onError = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toDeviceLocation(): DeviceLocation {
    val point = coordinate.useContents {
        GeoPoint(latitude = latitude, longitude = longitude)
    }
    return DeviceLocation(
        point = point,
        // Core Location reports negative values when unknown
        accuracyMeters = horizontalAccuracy.takeIf { it >= 0 }?.toFloat(),
        bearingDegrees = course.takeIf { it >= 0 }?.toFloat()
    )
}
