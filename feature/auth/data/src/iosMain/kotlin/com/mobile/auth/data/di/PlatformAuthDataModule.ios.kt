package com.mobile.auth.data.di

import com.mobile.auth.data.passkey.UnsupportedPasskeyAuthenticator
import com.mobile.auth.domain.passkey.PasskeyAuthenticator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformAuthDataModule = module {
    singleOf(::UnsupportedPasskeyAuthenticator) bind PasskeyAuthenticator::class
}
