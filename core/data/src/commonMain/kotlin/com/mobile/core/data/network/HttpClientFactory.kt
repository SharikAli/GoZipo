package com.mobile.core.data.network

import com.mobile.core.data.BuildKonfig
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.logging.GoZipoLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class HttpClientFactory(
    private val goZipoLogger: GoZipoLogger,
    private val sessionStorage: SessionStorage,
) {

    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }
                )
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            if (BuildKonfig.HTTP_LOGGING_ENABLED) {
                install(Logging) {
                    val revealSensitiveData = BuildKonfig.HTTP_LOG_SENSITIVE_DATA
                    logger = object : Logger {
                        override fun log(message: String) {
                            goZipoLogger.debug(
                                if (revealSensitiveData) message else HttpLogSanitizer.sanitize(message)
                            )
                        }
                    }
                    level = LogLevel.ALL
                    if (!revealSensitiveData) {
                        sanitizeHeader(placeholder = HttpLogSanitizer.MASK) { header ->
                            HttpLogSanitizer.isSensitiveHeader(header)
                        }
                    }
                }
            }
            install(WebSockets) {
                // Used by the Darwin engine (pings, and closes the socket after 2x without a pong).
                // OkHttp ignores it; Android's ping interval is set on the engine instead.
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage
                            .observeAuthInfo()
                            .firstOrNull()
                            ?.let {
                                BearerTokens(
                                    accessToken = it.accessToken,
                                    refreshToken = it.refreshToken
                                )
                            }
                    }
                    refreshTokens {
                        if (response.request.url.encodedPath.contains("auth/")) {
                            return@refreshTokens null
                        }

                        client.refreshSession(
                            sessionStorage = sessionStorage,
                            staleAccessToken = oldTokens?.accessToken,
                            builder = { markAsRefreshTokenRequest() }
                        )?.let {
                            BearerTokens(
                                accessToken = it.accessToken,
                                refreshToken = it.refreshToken
                            )
                        }
                    }
                }
            }
        }
    }
}