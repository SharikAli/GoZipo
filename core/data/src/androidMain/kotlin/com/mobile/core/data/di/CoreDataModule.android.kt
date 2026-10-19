package com.mobile.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mobile.core.data.auth.createDataStore
import com.mobile.core.data.location.FusedLocationProvider
import com.mobile.core.data.notification.FirebasePushNotificationService
import com.mobile.core.data.platform.CurrentActivityProvider
import com.mobile.core.data.realtime.AppLifecycleObserver
import com.mobile.core.data.realtime.ConnectionErrorHandler
import com.mobile.core.data.realtime.ConnectivityObserver
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.notification.PushNotificationService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

actual val platformCoreDataModule = module {
    single<HttpClientEngine> {
        OkHttp.create {
            // The realtime socket's keep-alive. OkHttp ignores the WebSockets plugin's
            // pingIntervalMillis and pings only when configured here; a missing pong fails
            // the socket, and KtorRealtimeClient reconnects. REST calls are unaffected.
            config { pingInterval(20, TimeUnit.SECONDS) }
        }
    }
    single<DataStore<Preferences>> {
        createDataStore(androidContext())
    }
    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    single(createdAtStart = true) { CurrentActivityProvider(androidApplication()) }
    singleOf(::AppLifecycleObserver)
    singleOf(::ConnectivityObserver)
    singleOf(::ConnectionErrorHandler)

    single<LocationProvider> { FusedLocationProvider(androidContext()) }
}
