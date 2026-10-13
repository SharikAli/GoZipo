package com.mobile.booking.presentation.util

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.place.Place
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** "Dolmen Mall Clifton, Block 4, Clifton"; blank for an unnamed dropped pin. */
fun Place.label(): String = listOf(name, address).filter { it.isNotBlank() }.joinToString(", ")

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
