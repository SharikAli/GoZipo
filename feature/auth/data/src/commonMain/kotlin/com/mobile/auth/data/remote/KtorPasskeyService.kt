package com.mobile.auth.data.remote

import com.mobile.auth.data.dto.requests.EmailRequest
import com.mobile.auth.data.dto.requests.LoginOptionsRequest
import com.mobile.auth.data.dto.requests.PasskeyVerifyRequest
import com.mobile.auth.data.dto.requests.SignupOptionsRequest
import com.mobile.auth.data.dto.responses.PasskeyAuthResponseDto
import com.mobile.auth.data.dto.responses.PasskeyCeremonyDto
import com.mobile.auth.data.mappers.toAuthInfo
import com.mobile.auth.data.mappers.toPasskeyCeremony
import com.mobile.auth.domain.passkey.PasskeyCeremony
import com.mobile.auth.domain.passkey.PasskeyService
import com.mobile.core.data.network.post
import com.mobile.core.domain.auth.AuthInfo
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.map
import io.ktor.client.HttpClient

class KtorPasskeyService(
    private val httpClient: HttpClient
) : PasskeyService {

    override suspend fun sendSignupCode(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post<EmailRequest, Unit>(
            route = "/auth/passkey/signup/code",
            body = EmailRequest(email)
        )
    }

    override suspend fun signupOptions(
        email: String,
        code: String
    ): Result<PasskeyCeremony, DataError.Remote> {
        return httpClient.post<SignupOptionsRequest, PasskeyCeremonyDto>(
            route = "/auth/passkey/signup/options",
            body = SignupOptionsRequest(
                email = email,
                code = code
            )
        ).map { it.toPasskeyCeremony() }
    }

    override suspend fun signupVerify(
        token: String,
        credentialJson: String
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient.post<PasskeyVerifyRequest, PasskeyAuthResponseDto>(
            route = "/auth/passkey/signup/verify",
            body = PasskeyVerifyRequest(
                token = token,
                credentialJson = credentialJson
            )
        ).map { it.toAuthInfo() }
    }

    override suspend fun loginOptions(email: String?): Result<PasskeyCeremony, DataError.Remote> {
        return httpClient.post<LoginOptionsRequest, PasskeyCeremonyDto>(
            route = "/auth/passkey/login/options",
            body = LoginOptionsRequest(email)
        ).map { it.toPasskeyCeremony() }
    }

    override suspend fun loginVerify(
        token: String,
        credentialJson: String
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient.post<PasskeyVerifyRequest, PasskeyAuthResponseDto>(
            route = "/auth/passkey/login/verify",
            body = PasskeyVerifyRequest(
                token = token,
                credentialJson = credentialJson
            )
        ).map { it.toAuthInfo() }
    }
}
