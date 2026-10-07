package com.mobile.core.map.mapbox

import com.mobile.core.data.network.safeCall
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.route.Route
import com.mobile.core.domain.route.RouteService
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.map.mapbox.dto.DirectionsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Directions API v5, `driving-traffic` profile (live traffic in the duration). */
class MapboxRouteService(
    private val httpClient: HttpClient,
    private val accessToken: String
) : RouteService {

    override suspend fun getRoute(from: GeoPoint, to: GeoPoint): Result<Route, DataError.Remote> {
        val response = safeCall<DirectionsResponseDto> {
            httpClient.get("$DIRECTIONS_URL/${from.toLngLat()};${to.toLngLat()}") {
                parameter("geometries", "geojson")
                parameter("overview", "full")
                parameter("access_token", accessToken)
            }
        }
        return when (response) {
            is Result.Failure -> response
            is Result.Success -> {
                // "NoRoute" / "NoSegment" come back as 200 with no routes
                val route = response.data.routes.firstOrNull()
                if (route != null) Result.Success(route.toRoute()) else Result.Failure(DataError.Remote.NOT_FOUND)
            }
        }
    }

    private companion object {
        const val DIRECTIONS_URL = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic"
    }
}
