package com.mobile.core.data.notification

import com.mobile.core.data.dto.requests.RegisterDeviceTokenRequest
import com.mobile.core.data.network.delete
import com.mobile.core.data.network.post
import com.mobile.core.domain.notification.DeviceTokenService
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import io.ktor.client.HttpClient

class KtorDeviceTokenService(
    private val httpClient: HttpClient
) : DeviceTokenService {

    override suspend fun registerToken(
        token: String,
        platform: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/notification/register",
            body = RegisterDeviceTokenRequest(
                token = token,
                platform = platform
            )
        )
    }

    override suspend fun unregisterToken(token: String): EmptyResult<DataError.Remote> {
        return httpClient.delete(
            route = "/notification/$token"
        )
    }
}