package com.mobile.core.map.mapbox

import com.mobile.core.data.network.safeCall
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.place.PlaceSearchService
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import com.mobile.core.map.mapbox.dto.SearchBoxResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Search Box API: `/forward` covers POIs (malls, hospitals) as well as addresses, which
 * the Geocoding API doesn't. Both endpoints are billed per request, no session token.
 */
class MapboxPlaceSearchService(
    private val httpClient: HttpClient,
    private val accessToken: String
) : PlaceSearchService {

    override suspend fun search(query: String, near: GeoPoint?): Result<List<Place>, DataError.Remote> {
        val text = query.trim()
        if (text.length < MIN_QUERY_LENGTH) {
            return Result.Success(emptyList())
        }

        return safeCall<SearchBoxResponseDto> {
            httpClient.get("$SEARCH_BOX_URL/forward") {
                parameter("q", text.take(MAX_QUERY_LENGTH))
                parameter("auto_complete", true)
                parameter("limit", SEARCH_LIMIT)
                parameter("country", MapboxConfig.COUNTRY)
                parameter("language", MapboxConfig.LANGUAGE)
                near?.let { parameter("proximity", it.toLngLat()) }
                parameter("access_token", accessToken)
            }
        }.map { response -> response.features.map { it.toPlace() } }
    }

    override suspend fun reverseGeocode(point: GeoPoint): Result<Place?, DataError.Remote> {
        return safeCall<SearchBoxResponseDto> {
            httpClient.get("$SEARCH_BOX_URL/reverse") {
                parameter("longitude", point.longitude)
                parameter("latitude", point.latitude)
                parameter("limit", 1)
                parameter("language", MapboxConfig.LANGUAGE)
                parameter("access_token", accessToken)
            }
        }.map { response -> response.features.firstOrNull()?.toPlace() }
    }

    private companion object {
        const val SEARCH_BOX_URL = "https://api.mapbox.com/search/searchbox/v1"
        const val MIN_QUERY_LENGTH = 2
        const val MAX_QUERY_LENGTH = 256
        const val SEARCH_LIMIT = 10
    }
}
