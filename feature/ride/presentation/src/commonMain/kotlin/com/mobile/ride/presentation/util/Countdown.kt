package com.mobile.ride.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Length of one search round, for countdown progress. The backend decides the actual end. */
val SEARCH_ROUND = 60.seconds

/** Time left until [expiresAt], ticking every second; zero once it passed or when there is none. */
@Composable
fun rememberRemaining(expiresAt: Instant?): Duration {
    var remaining by remember(expiresAt) { mutableStateOf(expiresAt.remaining()) }
    LaunchedEffect(expiresAt) {
        while (expiresAt != null && remaining > Duration.ZERO) {
            delay(1.seconds)
            remaining = expiresAt.remaining()
        }
    }
    return remaining
}

/** "0:58". */
fun Duration.toCountdownText(): String {
    val seconds = inWholeSeconds
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private fun Instant?.remaining(): Duration =
    this?.let { (it - Clock.System.now()).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO
