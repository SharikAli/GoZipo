package com.mobile.auth.data.di

import com.mobile.auth.data.remote.KtorAuthService
import com.mobile.auth.data.remote.KtorPasskeyService
import com.mobile.auth.domain.authservice.AuthService
import com.mobile.auth.domain.passkey.PasskeyService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformAuthDataModule: Module

val authDataModule = module {
    includes(platformAuthDataModule)
    singleOf(::KtorAuthService) bind AuthService::class
    singleOf(::KtorPasskeyService) bind PasskeyService::class
}
