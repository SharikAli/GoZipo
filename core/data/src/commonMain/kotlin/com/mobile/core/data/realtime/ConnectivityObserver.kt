package com.mobile.core.data.realtime

import kotlinx.coroutines.flow.Flow

expect class ConnectivityObserver {
    val isConnected: Flow<Boolean>
}