package com.mobile.auth.data.di

import com.mobile.auth.data.remote.KtorAuthService
import com.mobile.auth.domain.authservice.AuthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authDataModule = module {
    singleOf(::KtorAuthService) bind AuthService::class
}