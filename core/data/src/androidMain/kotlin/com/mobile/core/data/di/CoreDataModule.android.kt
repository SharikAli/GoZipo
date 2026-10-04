package com.mobile.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mobile.core.data.auth.createDataStore
import com.mobile.core.data.notification.FirebasePushNotificationService
import com.mobile.core.data.platform.CurrentActivityProvider
import com.mobile.core.domain.notification.PushNotificationService
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<DataStore<Preferences>> {
        createDataStore(androidContext())
    }
    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    single(createdAtStart = true) { CurrentActivityProvider(androidApplication()) }
}