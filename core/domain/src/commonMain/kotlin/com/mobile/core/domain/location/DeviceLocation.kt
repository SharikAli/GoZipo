package com.mobile.core.domain.location

/**
 * A fix from the device's location provider.
 *
 * @param accuracyMeters horizontal accuracy radius, when known
 * @param bearingDegrees direction of travel (0-360, clockwise from north), when moving
 */
data class DeviceLocation(
    val point: GeoPoint,
    val accuracyMeters: Float?,
    val bearingDegrees: Float?
)
