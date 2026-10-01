package com.mobile.gozipo.di

import com.mobile.auth.data.di.authDataModule
import com.mobile.auth.presentation.di.authPresentationModule
import com.mobile.core.data.di.coreDataModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appModule,
            coreDataModule,
            authDataModule,
            authPresentationModule
        )
    }
}