package com.mobile.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mobile.core.domain.location.DeviceLocation
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** [LocationProvider] backed by Google Play services' fused location provider. */
class FusedLocationProvider(
    private val context: Context
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(LocationManager::class.java)

    override fun checkAvailability(): EmptyResult<LocationError> {
        return when {
            !hasPermission() -> Result.Failure(LocationError.PERMISSION_DENIED)
            !isLocationEnabled() -> Result.Failure(LocationError.SERVICES_DISABLED)
            else -> Result.Success(Unit)
        }
    }

    @SuppressLint("MissingPermission") // checked by checkAvailability()
    override suspend fun getCurrentLocation(): Result<DeviceLocation, LocationError> {
        val availability = checkAvailability()
        if (availability is Result.Failure) {
            return availability
        }

        val location = currentLocation() ?: lastLocation()
        return if (location != null) {
            Result.Success(location.toDeviceLocation())
        } else {
            Result.Failure(LocationError.UNAVAILABLE)
        }
    }

    @SuppressLint("MissingPermission") // checked by checkAvailability()
    override fun observeLocation(intervalMillis: Long): Flow<DeviceLocation> {
        if (checkAvailability() is Result.Failure) {
            return emptyFlow()
        }

        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(intervalMillis / 2)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(it.toDeviceLocation()) }
                }
            }

            client.requestLocationUpdates(request, callback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resume(null) }
        continuation.invokeOnCancellation { cancellation.cancel() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resume(null) }
    }

    private fun hasPermission(): Boolean {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

private fun Location.toDeviceLocation() = DeviceLocation(
    point = GeoPoint(latitude = latitude, longitude = longitude),
    accuracyMeters = if (hasAccuracy()) accuracy else null,
    bearingDegrees = if (hasBearing()) bearing else null
)
