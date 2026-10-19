package com.mobile.ride.data

import kotlinx.serialization.json.Json

/** For bodies decoded outside Ktor's content negotiation (realtime payloads, optional bodies). */
internal val RideJson = Json { ignoreUnknownKeys = true }
