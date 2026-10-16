package com.mobile.auth.data.di

import com.mobile.auth.data.passkey.AuthenticationServicesPasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformAuthDataModule = module {
    singleOf(::AuthenticationServicesPasskeyAuthenticator) bind PasskeyAuthenticator::class
}
