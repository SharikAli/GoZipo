package com.mobile.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mobile.core.data.auth.createDataStore
import com.mobile.core.data.notification.FirebasePushNotificationService
import com.mobile.core.data.location.CoreLocationProvider
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.data.realtime.AppLifecycleObserver
import com.mobile.core.data.realtime.ConnectionErrorHandler
import com.mobile.core.data.realtime.ConnectivityObserver
import com.mobile.core.domain.notification.PushNotificationService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule = module {
    single<HttpClientEngine> { Darwin.create() }
    single<DataStore<Preferences>> {
        createDataStore()
    }
    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    singleOf(::ConnectivityObserver)
    singleOf(::AppLifecycleObserver)
    singleOf(::ConnectionErrorHandler)
    singleOf(::CoreLocationProvider) bind LocationProvider::class
}
