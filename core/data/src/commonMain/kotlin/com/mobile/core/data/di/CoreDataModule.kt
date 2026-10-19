package com.mobile.core.data.di

import com.mobile.core.data.auth.DataStoreSessionStorage
import com.mobile.core.data.logging.KermitLogger
import com.mobile.core.data.network.HttpClientFactory
import com.mobile.core.data.notification.KtorDeviceTokenService
import com.mobile.core.data.realtime.ConnectionErrorHandler
import com.mobile.core.data.realtime.ConnectionRetryHandler
import com.mobile.core.data.realtime.KtorRealtimeClient
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.logging.GoZipoLogger
import com.mobile.core.domain.notification.DeviceTokenService
import com.mobile.core.domain.realtime.RealtimeClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
    single<GoZipoLogger> { KermitLogger }
    single {
        HttpClientFactory(get(), get()).create(get())
    }
    singleOf(::DataStoreSessionStorage) bind SessionStorage::class
    singleOf(::KtorDeviceTokenService) bind DeviceTokenService::class

    singleOf(::ConnectionRetryHandler)
    single<RealtimeClient> {
        KtorRealtimeClient(
            httpClient = get(),
            sessionStorage = get(),
            connectionErrorHandler = get(),
            connectionRetryHandler = get(),
            appLifecycleObserver = get(),
            connectivityObserver = get(),
            applicationScope = get(),
            logger = get()
        )
    }
}