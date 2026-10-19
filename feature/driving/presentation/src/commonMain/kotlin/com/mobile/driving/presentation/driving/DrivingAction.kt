package com.mobile.driving.presentation.driving

import com.mobile.core.presentation.permissions.PermissionState

sealed interface DrivingAction {
    data class OnLocationPermissionResult(val state: PermissionState) : DrivingAction

    /** The screen came back to the foreground, e.g. from the vehicle screen. */
    data object OnResume : DrivingAction
    data object OnRecenterClick : DrivingAction
    data object OnLogoutClick : DrivingAction
    data object OnVehicleClick : DrivingAction
    data object OnEarningsClick : DrivingAction
    data object OnRetryStatusClick : DrivingAction

    data object OnGoOnlineClick : DrivingAction
    data object OnGoOfflineClick : DrivingAction

    data class OnRequestClick(val rideId: String) : DrivingAction
    data object OnDecreaseOfferClick : DrivingAction
    data object OnIncreaseOfferClick : DrivingAction
    data object OnSubmitOfferClick : DrivingAction
    data class OnWithdrawOfferClick(val rideId: String, val offerId: String) : DrivingAction

    data object OnNavigateClick : DrivingAction
    data object OnArrivedClick : DrivingAction
    data object OnStartTripClick : DrivingAction
    data object OnCompleteTripClick : DrivingAction
    data object OnCancelRideClick : DrivingAction
    data object OnFinishedDismiss : DrivingAction
}
