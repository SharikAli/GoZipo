package com.mobile.driving.presentation.history

import com.mobile.core.presentation.util.UiText
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RiderEarnings

data class RideHistoryState(
    val earnings: RiderEarnings? = null,
    val rides: List<Ride> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val errorText: UiText? = null,
)
