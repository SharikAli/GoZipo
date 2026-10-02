package com.mobile.auth.data.mappers

import com.mobile.auth.data.dto.responses.PasskeyAuthResponseDto
import com.mobile.auth.data.dto.responses.PasskeyCeremonyDto
import com.mobile.auth.domain.passkey.PasskeyCeremony
import com.mobile.core.data.mappers.toUser
import com.mobile.core.domain.auth.AuthInfo

fun PasskeyCeremonyDto.toPasskeyCeremony(): PasskeyCeremony {
    return PasskeyCeremony(
        token = token,
        requestJson = requestJson
    )
}

fun PasskeyAuthResponseDto.toAuthInfo(): AuthInfo {
    return AuthInfo(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toUser()
    )
}
