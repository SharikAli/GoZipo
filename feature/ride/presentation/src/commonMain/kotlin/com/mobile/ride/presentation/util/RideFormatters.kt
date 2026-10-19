package com.mobile.ride.presentation.util

import androidx.compose.runtime.Composable
import com.mobile.core.domain.location.GeoPoint
import gozipo.feature.ride.presentation.generated.resources.Res
import gozipo.feature.ride.presentation.generated.resources.distance_kilometers
import gozipo.feature.ride.presentation.generated.resources.distance_meters
import gozipo.feature.ride.presentation.generated.resources.fare_amount
import gozipo.feature.ride.presentation.generated.resources.trip_minutes
import org.jetbrains.compose.resources.stringResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** 1234 → "1,234". Fares are whole PKR. */
fun formatAmount(amount: Int): String {
    val digits = amount.toString()
    return digits.reversed().chunked(3).joinToString(",").reversed()
}

/** One decimal for kilometres ("12.4"). */
fun formatKilometers(meters: Int): String {
    val tenths = (meters / 100.0).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** "PKR1,234". */
@Composable
fun fareText(amount: Int): String = stringResource(Res.string.fare_amount, formatAmount(amount))

/** "850 m" below a kilometre, otherwise "2.4 km". */
@Composable
fun distanceText(meters: Int): String = if (meters < 1_000) {
    stringResource(Res.string.distance_meters, meters)
} else {
    stringResource(Res.string.distance_kilometers, formatKilometers(meters))
}

/** "22 min", at least one minute. */
@Composable
fun minutesText(duration: Duration): String =
    stringResource(Res.string.trip_minutes, duration.inWholeMinutes.coerceAtLeast(1))

/** Great-circle distance; good enough for "is this the same spot". */
fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val lat1 = latitude.toRadians()
    val lat2 = other.latitude.toRadians()
    val dLat = lat2 - lat1
    val dLon = (other.longitude - longitude).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0
