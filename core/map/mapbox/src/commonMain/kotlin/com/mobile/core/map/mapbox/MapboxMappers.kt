package com.mobile.core.map.mapbox

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.route.Route
import com.mobile.core.map.mapbox.dto.DirectionsRouteDto
import com.mobile.core.map.mapbox.dto.SearchBoxFeatureDto
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

internal fun SearchBoxFeatureDto.toPlace(): Place = with(properties) {
    Place(
        id = mapboxId,
        name = name,
        address = placeFormatted ?: fullAddress.orEmpty(),
        point = GeoPoint(latitude = coordinates.latitude, longitude = coordinates.longitude)
    )
}

internal fun DirectionsRouteDto.toRoute(): Route = Route(
    distanceMeters = distance.roundToInt(),
    duration = duration.seconds,
    points = geometry.coordinates.map { (longitude, latitude) ->
        GeoPoint(latitude = latitude, longitude = longitude)
    }
)

/** Mapbox writes coordinates longitude first. */
internal fun GeoPoint.toLngLat(): String = "$longitude,$latitude"
