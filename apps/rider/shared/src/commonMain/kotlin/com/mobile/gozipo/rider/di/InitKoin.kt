package com.mobile.gozipo.rider.di

import com.mobile.apps.common.di.appCommonModule
import com.mobile.auth.data.di.authDataModule
import com.mobile.auth.presentation.di.authPresentationModule
import com.mobile.core.data.di.coreDataModule
import com.mobile.core.domain.auth.UserRole
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appCommonModule(UserRole.RIDER),
            coreDataModule,
            authDataModule,
            authPresentationModule
        )
    }
}