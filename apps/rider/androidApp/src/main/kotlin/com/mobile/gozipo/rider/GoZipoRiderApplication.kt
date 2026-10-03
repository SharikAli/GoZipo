package com.mobile.gozipo.rider

import android.app.Application
import com.mobile.gozipo.rider.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class GoZipoRiderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GoZipoRiderApplication)
            androidLogger()
        }
    }
}