package com.mobile.core.data.realtime

import kotlinx.coroutines.flow.Flow

expect class AppLifecycleObserver {
    val isInForeground: Flow<Boolean>
}
