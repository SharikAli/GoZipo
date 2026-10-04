package com.mobile.auth.data.remote

import com.mobile.auth.data.dto.requests.ChangePasswordRequest
import com.mobile.auth.data.dto.requests.EmailRequest
import com.mobile.auth.data.dto.requests.LoginRequest
import com.mobile.auth.data.dto.requests.RegisterRequest
import com.mobile.auth.data.dto.requests.ResetPasswordRequest
import com.mobile.auth.domain.authservice.AuthService
import com.mobile.core.data.dto.AuthInfoSerializable
import com.mobile.core.data.dto.requests.RefreshRequest
import com.mobile.core.data.mappers.toAuthInfo
import com.mobile.core.data.network.get
import com.mobile.core.data.network.post
import com.mobile.core.domain.auth.AuthInfo
import com.mobile.core.domain.auth.UserRole
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

class KtorAuthService(
    private val httpClient: HttpClient,
    private val appRole: UserRole
) : AuthService {

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient.post<LoginRequest, AuthInfoSerializable>(
            route = "/auth/login",
            body = LoginRequest(
                email = email,
                password = password,
                role = appRole
            )
        ).map { authInfoSerializable ->
            authInfoSerializable.toAuthInfo()
        }
    }

    override suspend fun register(
        email: String,
        username: String,
        password: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/auth/register",
            body = RegisterRequest(
                email = email,
                username = username,
                password = password,
                role = appRole
            )
        )
    }

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/auth/resend-verification",
            body = EmailRequest(email),
        )
    }

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
        return httpClient.get(
            route = "/auth/verify",
            queryParams = mapOf("token" to token)
        )
    }

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post<EmailRequest, Unit>(
            route = "/auth/forgot-password",
            body = EmailRequest(email)
        )
    }

    override suspend fun resetPassword(
        newPassword: String,
        token: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/auth/reset-password",
            body = ResetPasswordRequest(
                newPassword = newPassword,
                token = token
            )
        )
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/auth/change-password",
            body = ChangePasswordRequest(
                oldPassword = currentPassword,
                newPassword = newPassword
            )
        )
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        return httpClient.post<RefreshRequest, Unit>(
            route = "/auth/logout",
            body = RefreshRequest(refreshToken)
        ).also {
            // Drop cached tokens even if the request failed: the session is cleared locally anyway
            httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        }
    }
}