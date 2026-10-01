package com.mobile.gozipo

import android.app.Application
import com.mobile.gozipo.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class GoZipoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@GoZipoApplication)
            androidLogger()
        }
    }
}