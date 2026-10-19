package com.mobile.driving.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.util.onSuccess
import com.mobile.ride.domain.RideEvent
import com.mobile.ride.domain.RideEventSource
import com.mobile.ride.domain.RideService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Keeps guidance tied to the ride: closes it when the ride ends (e.g. the customer cancels)
 * while the rider is navigating. Everything else stays with the driving screen behind it.
 */
class TurnByTurnViewModel(
    private val rideId: String,
    private val rideService: RideService,
    private val rideEventSource: RideEventSource,
) : ViewModel() {

    private val eventChannel = Channel<TurnByTurnEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            rideEventSource.events.collect { event ->
                if (event is RideEvent.RideUpdated && event.ride.id == rideId && !event.ride.status.isActive) {
                    eventChannel.send(TurnByTurnEvent.RideEnded)
                }
            }
        }
        // Missed events aren't replayed: check the ride after every (re)connect
        viewModelScope.launch {
            rideEventSource.connectionState
                .map { it == ConnectionState.CONNECTED }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    rideService.getRide(rideId).onSuccess { ride ->
                        if (!ride.status.isActive) eventChannel.send(TurnByTurnEvent.RideEnded)
                    }
                }
        }
    }
}

sealed interface TurnByTurnEvent {
    data object RideEnded : TurnByTurnEvent
}
