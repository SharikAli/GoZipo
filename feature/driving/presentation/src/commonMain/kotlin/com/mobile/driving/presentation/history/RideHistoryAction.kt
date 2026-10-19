package com.mobile.driving.presentation.history

sealed interface RideHistoryAction {
    data object OnBackClick : RideHistoryAction
    data object OnRetryClick : RideHistoryAction
    data object OnLoadMore : RideHistoryAction
}
