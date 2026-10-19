package com.mobile.core.data.realtime

import com.mobile.core.domain.realtime.ConnectionState

expect class ConnectionErrorHandler() {
    fun getConnectionStateForError(cause: Throwable): ConnectionState
    fun transformException(exception: Throwable): Throwable
    fun isRetriableError(cause: Throwable): Boolean
}
