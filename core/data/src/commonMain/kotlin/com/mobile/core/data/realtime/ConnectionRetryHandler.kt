package com.mobile.core.data.realtime

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Backoff between reconnects: 2 s, 4 s, 8 s, 16 s, then 30 s. Unlike the attempt count of
 * `retryWhen`, the count here starts over after every successful connection.
 */
class ConnectionRetryHandler(
    private val connectionErrorHandler: ConnectionErrorHandler
) {
    private var attempt = 0
    private var shouldSkipBackoff = false

    fun shouldRetry(cause: Throwable): Boolean {
        return cause is RealtimeAuthenticationException ||
                cause is RealtimeConnectionClosedException ||
                connectionErrorHandler.isRetriableError(cause)
    }

    suspend fun applyRetryDelay() {
        if (shouldSkipBackoff) {
            shouldSkipBackoff = false
        } else {
            delay(createBackoffDelay(attempt++))
        }
    }

    fun onConnected() {
        attempt = 0
    }

    /** Reconnect without waiting, e.g. when the app comes back to the foreground. */
    fun resetDelay() {
        shouldSkipBackoff = true
    }

    private fun createBackoffDelay(attempt: Int): Long {
        val delayTime = (2f.pow(attempt.coerceAtMost(10)) * 2_000L).toLong()
        return minOf(delayTime, MAX_DELAY_MILLIS)
    }

    private companion object {
        const val MAX_DELAY_MILLIS = 30_000L
    }
}
