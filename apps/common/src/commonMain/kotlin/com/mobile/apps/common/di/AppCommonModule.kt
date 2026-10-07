package com.mobile.apps.common.di

import com.mobile.apps.common.MainViewModel
import com.mobile.apps.common.home.HomeMapViewModel
import com.mobile.core.domain.auth.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * @param appRole the account role this app signs in as; auth requests send it
 * and the backend rejects accounts of the other role.
 */
fun appCommonModule(appRole: UserRole) = module {
    single { appRole }
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeMapViewModel)
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}