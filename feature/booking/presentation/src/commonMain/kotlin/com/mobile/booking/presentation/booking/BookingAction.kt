package com.mobile.booking.presentation.booking

import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.place.Place
import com.mobile.core.presentation.permissions.PermissionState
import com.mobile.ride.domain.VehicleType

sealed interface BookingAction {
    data class OnLocationPermissionResult(val state: PermissionState) : BookingAction
    data class OnCameraIdle(val center: GeoPoint) : BookingAction
    data class OnMapClick(val point: GeoPoint) : BookingAction
    data object OnRecenterClick : BookingAction
    data object OnLogoutClick : BookingAction

    data class OnOpenSearch(val target: SearchTarget) : BookingAction
    data object OnCloseSearch : BookingAction
    data class OnPlaceSelected(val place: Place) : BookingAction
    data object OnChooseOnMapClick : BookingAction
    data object OnClearDropoffClick : BookingAction
    data object OnConfirmDestinationPinClick : BookingAction
    data object OnCancelDestinationPinClick : BookingAction

    data class OnVehicleSelected(val vehicleType: VehicleType) : BookingAction
    data object OnRetryEstimatesClick : BookingAction
    data object OnDecreaseFareClick : BookingAction
    data object OnIncreaseFareClick : BookingAction
    data class OnAutoAcceptChange(val enabled: Boolean) : BookingAction
    data object OnFindOffersClick : BookingAction

    data object OnConfirmFareClick : BookingAction
    data class OnAcceptOfferClick(val offerId: String) : BookingAction
    data class OnDeclineOfferClick(val offerId: String) : BookingAction
    data object OnCancelRideClick : BookingAction

    data object OnFinishedDismiss : BookingAction
}
