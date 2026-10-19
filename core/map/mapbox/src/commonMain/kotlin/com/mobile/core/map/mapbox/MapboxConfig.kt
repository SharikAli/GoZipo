package com.mobile.core.map.mapbox

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal object MapboxConfig {
    /** Search results are limited to Pakistan. */
    const val COUNTRY = "PK"
    const val LANGUAGE = "en"
}

/**
 * Client for Mapbox only. Deliberately not the app's main client: that one adds our API key
 * and the user's bearer token to every request, and logs URLs (which carry the Mapbox token).
 */
internal fun createMapboxHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000L
        }
    }
}
