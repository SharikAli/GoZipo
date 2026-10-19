package com.mobile.core.domain.realtime

import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's single realtime connection to the backend (`/ws/rides`).
 *
 * The socket is open while [messages] has collectors, a user is signed in, the app is in
 * the foreground and the device is online. It reconnects with backoff after network errors
 * and refreshes the access token when the server rejects it. Messages missed while
 * disconnected are not replayed: resync from the REST API when [connectionState]
 * becomes [ConnectionState.CONNECTED] again.
 */
interface RealtimeClient {
    val connectionState: StateFlow<ConnectionState>

    /** Incoming messages, shared by all collectors over one socket. */
    val messages: Flow<RealtimeMessage>

    suspend fun send(message: RealtimeMessage): EmptyResult<DataError.Connection>
}
