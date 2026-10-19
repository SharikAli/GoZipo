package com.mobile.core.map.mapbox

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MapboxServicesTest {

    private val requests = mutableListOf<HttpRequestData>()

    // runTest's virtual clock would fire the client's HttpTimeout immediately
    private fun realTimeTest(block: suspend () -> Unit) = runTest {
        withContext(Dispatchers.Default) { block() }
    }

    private fun engine(status: HttpStatusCode = HttpStatusCode.OK, body: String) = MockEngine { request ->
        requests += request
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private fun search(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MapboxPlaceSearchService(createMapboxHttpClient(engine(status, body)), "pk.test")

    private fun routes(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MapboxRouteService(createMapboxHttpClient(engine(status, body)), "pk.test")

    @Test
    fun searchSendsQueryBiasAndTokenAndMapsPlaces() = realTimeTest {
        val result = search(SEARCH_RESPONSE).search("dolmen", near = GeoPoint(24.94, 67.11))

        val url = requests.single().url
        assertEquals("/search/searchbox/v1/forward", url.encodedPath)
        assertEquals("dolmen", url.parameters["q"])
        assertEquals("67.11,24.94", url.parameters["proximity"])
        assertEquals("PK", url.parameters["country"])
        assertEquals("true", url.parameters["auto_complete"])
        assertEquals("pk.test", url.parameters["access_token"])
        // Never our backend credentials
        assertNull(requests.single().headers["x-api-key"])
        assertNull(requests.single().headers[HttpHeaders.Authorization])

        val places = (result as Result.Success).data
        assertEquals(2, places.size)
        with(places.first()) {
            assertEquals("dXJuOm1ieHBvaToxMjM", id)
            assertEquals("Dolmen Mall Clifton", name)
            assertEquals("Block 4, Clifton, Karachi, Sindh", address)
            assertEquals(GeoPoint(24.8023, 67.0297), point)
        }
        // Falls back to full_address without place_formatted
        assertEquals("Tariq Road, Karachi", places[1].address)
    }

    @Test
    fun shortQueriesDontCallMapbox() = realTimeTest {
        val result = search(SEARCH_RESPONSE).search(" d ", near = null)

        assertEquals(Result.Success(emptyList()), result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun reverseGeocodeReturnsFirstFeatureOrNull() = realTimeTest {
        val found = search(SEARCH_RESPONSE).reverseGeocode(GeoPoint(24.8023, 67.0297))
        assertEquals("/search/searchbox/v1/reverse", requests.last().url.encodedPath)
        assertEquals("67.0297", requests.last().url.parameters["longitude"])
        assertEquals("Dolmen Mall Clifton", (found as Result.Success).data?.name)

        val empty = search("""{"type":"FeatureCollection","features":[]}""").reverseGeocode(GeoPoint(0.0, 0.0))
        assertEquals(Result.Success(null), empty)
    }

    @Test
    fun invalidTokenMapsToUnauthorized() = realTimeTest {
        val result = search("""{"message":"Not Authorized - Invalid Token"}""", HttpStatusCode.Unauthorized)
            .search("dolmen", near = null)

        assertEquals(Result.Failure(DataError.Remote.UNAUTHORIZED), result)
    }

    @Test
    fun routeUsesLngLatPathAndMapsGeometry() = realTimeTest {
        val result = routes(DIRECTIONS_RESPONSE).getRoute(GeoPoint(24.9425, 67.114), GeoPoint(24.88, 67.06))

        val url = requests.single().url
        assertEquals(
            "/directions/v5/mapbox/driving-traffic/67.114,24.9425;67.06,24.88",
            url.encodedPath
        )
        assertEquals("geojson", url.parameters["geometries"])
        assertEquals("full", url.parameters["overview"])

        val route = (result as Result.Success).data
        assertEquals(10_342, route.distanceMeters)
        assertEquals(1_215.4.seconds, route.duration)
        assertEquals(listOf(GeoPoint(24.9425, 67.114), GeoPoint(24.91, 67.09), GeoPoint(24.88, 67.06)), route.points)
    }

    @Test
    fun noRouteMapsToNotFound() = realTimeTest {
        val result = routes("""{"code":"NoRoute","message":"No route found","routes":[]}""")
            .getRoute(GeoPoint(24.9, 67.1), GeoPoint(0.0, 0.0))

        assertEquals(Result.Failure(DataError.Remote.NOT_FOUND), result)
    }

    private companion object {
        val SEARCH_RESPONSE = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","geometry":{"type":"Point","coordinates":[67.0297,24.8023]},
               "properties":{"name":"Dolmen Mall Clifton","mapbox_id":"dXJuOm1ieHBvaToxMjM","feature_type":"poi",
                 "full_address":"Dolmen Mall Clifton, Block 4, Clifton, Karachi, Sindh",
                 "place_formatted":"Block 4, Clifton, Karachi, Sindh",
                 "coordinates":{"latitude":24.8023,"longitude":67.0297,"routable_points":[]},
                 "poi_category":["shopping"]}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[67.05,24.87]},
               "properties":{"name":"Tariq Road","mapbox_id":"abc","feature_type":"street",
                 "full_address":"Tariq Road, Karachi",
                 "coordinates":{"latitude":24.87,"longitude":67.05}}}
            ],"attribution":"© 2026 Mapbox"}
        """.trimIndent()

        val DIRECTIONS_RESPONSE = """
            {"code":"Ok","uuid":"x","waypoints":[],
             "routes":[{"weight_name":"auto","weight":1300.1,"duration":1215.4,"distance":10341.7,"legs":[],
               "geometry":{"type":"LineString","coordinates":[[67.114,24.9425],[67.09,24.91],[67.06,24.88]]}}]}
        """.trimIndent()
    }
}
