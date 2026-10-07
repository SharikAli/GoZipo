package com.mobile.core.data.network

import com.mobile.core.data.dto.AuthInfoSerializable
import com.mobile.core.data.dto.requests.RefreshRequest
import com.mobile.core.data.mappers.toAuthInfo
import com.mobile.core.domain.auth.AuthInfo
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Shared by the REST client and the realtime socket: refresh tokens rotate, so two
// refreshes racing with the same refresh token would log the user out.
private val refreshMutex = Mutex()

/**
 * Exchanges the stored refresh token for new tokens and saves them.
 *
 * @param staleAccessToken the access token the caller saw rejected. If the stored one
 * already differs, another caller refreshed in the meantime and that session is returned.
 * @param builder extra request setup; the Auth plugin's refresh passes `markAsRefreshTokenRequest()`.
 * Calls from outside the plugin don't need it, since 401s on `auth/` routes are never refreshed.
 * @return the new session, or null when there is none (the session is then cleared).
 */
suspend fun HttpClient.refreshSession(
    sessionStorage: SessionStorage,
    staleAccessToken: String?,
    builder: HttpRequestBuilder.() -> Unit = {},
): AuthInfo? = refreshMutex.withLock {
    val authInfo = sessionStorage.observeAuthInfo().firstOrNull()
    if (authInfo != null && staleAccessToken != null && authInfo.accessToken != staleAccessToken) {
        return@withLock authInfo
    }
    if (authInfo?.refreshToken.isNullOrBlank()) {
        sessionStorage.set(null)
        return@withLock null
    }

    var refreshed: AuthInfo? = null
    post<RefreshRequest, AuthInfoSerializable>(
        route = "/auth/refresh",
        body = RefreshRequest(refreshToken = authInfo.refreshToken),
        builder = builder
    ).onSuccess { newAuthInfo ->
        refreshed = newAuthInfo.toAuthInfo()
        sessionStorage.set(refreshed)
    }.onFailure { _ ->
        sessionStorage.set(null)
    }
    refreshed
}
