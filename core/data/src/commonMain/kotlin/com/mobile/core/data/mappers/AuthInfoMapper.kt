package com.mobile.core.data.mappers

import com.mobile.core.data.dto.AuthInfoSerializable
import com.mobile.core.data.dto.UserSerializable
import com.mobile.core.domain.auth.AuthInfo
import com.mobile.core.domain.auth.User

fun AuthInfoSerializable.toAuthInfo(): AuthInfo {
    return AuthInfo(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toUser()
    )
}

fun UserSerializable.toUser(): User {
    return User(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        role = role
    )
}

fun User.toSerializable(): UserSerializable {
    return UserSerializable(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        role = role
    )
}

fun AuthInfo.toSerializable(): AuthInfoSerializable {
    return AuthInfoSerializable(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toSerializable()
    )
}