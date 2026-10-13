package com.mobile.ride.domain

import com.mobile.core.domain.realtime.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Ride events from the app's realtime socket. Collecting [events] keeps the socket open. */
interface RideEventSource {
    val events: Flow<RideEvent>

    /** Resync over REST whenever this becomes [ConnectionState.CONNECTED] again. */
    val connectionState: StateFlow<ConnectionState>
}
