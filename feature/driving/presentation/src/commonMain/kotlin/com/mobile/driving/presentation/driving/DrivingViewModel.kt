package com.mobile.driving.presentation.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.route.RouteService
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.presentation.permissions.PermissionState
import com.mobile.core.presentation.util.UiText
import com.mobile.core.presentation.util.toUiText
import com.mobile.ride.domain.OfferStatus
import com.mobile.ride.domain.OpenRide
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideEvent
import com.mobile.ride.domain.RideEventSource
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideService
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.RiderService
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.error_offer_out_of_range
import gozipo.feature.driving.presentation.generated.resources.error_ride_changed
import gozipo.feature.driving.presentation.generated.resources.error_ride_not_available
import gozipo.feature.driving.presentation.generated.resources.error_went_offline
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The rider's map home: availability, live requests and offers, then the trip steps.
 * The backend owns ride state and who is online; this mirrors it from REST responses and
 * realtime events, and resyncs whenever the socket (re)connects or the screen resumes.
 */
class DrivingViewModel(
    private val locationProvider: LocationProvider,
    private val routeService: RouteService,
    private val rideService: RideService,
    private val riderService: RiderService,
    private val rideEventSource: RideEventSource,
) : ViewModel() {

    private val eventChannel = Channel<DrivingEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false
    private var hasPromptedForVehicle = false

    /** Go online once location permission comes back. */
    private var goOnlineAfterPermission = false
    private var locationJob: Job? = null
    private var routeJob: Job? = null

    private val _state = MutableStateFlow(DrivingState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadLocation(requestPermission = true)
                observeRideEvents()
                observeConnection()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: DrivingAction) {
        when (action) {
            is DrivingAction.OnLocationPermissionResult -> onLocationPermissionResult(action.state)
            DrivingAction.OnResume -> loadStatus()
            DrivingAction.OnRecenterClick -> recenter()
            DrivingAction.OnRetryStatusClick -> loadStatus()
            DrivingAction.OnLogoutClick,
            DrivingAction.OnVehicleClick,
            DrivingAction.OnNavigateClick,
            DrivingAction.OnEarningsClick -> Unit

            DrivingAction.OnGoOnlineClick -> goOnline()
            DrivingAction.OnGoOfflineClick -> goOffline()

            is DrivingAction.OnRequestClick -> selectRequest(action.rideId)
            DrivingAction.OnDecreaseOfferClick -> changeOffer(-1)
            DrivingAction.OnIncreaseOfferClick -> changeOffer(+1)
            DrivingAction.OnSubmitOfferClick -> submitOffer()
            is DrivingAction.OnWithdrawOfferClick -> withdrawOffer(action.rideId, action.offerId)

            DrivingAction.OnArrivedClick -> tripStep { rideService.markArrived(it) }
            DrivingAction.OnStartTripClick -> tripStep { rideService.startTrip(it) }
            DrivingAction.OnCompleteTripClick -> tripStep { rideService.completeTrip(it) }
            DrivingAction.OnCancelRideClick -> cancelRide()
            DrivingAction.OnFinishedDismiss -> dismissFinished()
        }
    }

    // Location

    private fun onLocationPermissionResult(permission: PermissionState) {
        if (permission == PermissionState.GRANTED) {
            loadLocation(requestPermission = false)
            if (goOnlineAfterPermission) goOnline()
        } else {
            _state.update { it.copy(locationError = LocationError.PERMISSION_DENIED) }
        }
        goOnlineAfterPermission = false
    }

    private fun loadLocation(requestPermission: Boolean) {
        viewModelScope.launch { currentLocation(requestPermission) }
    }

    /** A fresh fix (also stored in the state), or null after asking for permission when allowed. */
    private suspend fun currentLocation(requestPermission: Boolean): GeoPoint? {
        return when (val result = locationProvider.getCurrentLocation()) {
            is Result.Success -> {
                val firstFix = _state.value.userLocation == null
                _state.update {
                    it.copy(
                        userLocation = result.data.point,
                        userBearing = result.data.bearingDegrees,
                        locationError = null
                    )
                }
                if (firstFix && _state.value.ride == null) {
                    eventChannel.send(DrivingEvent.CenterOn(result.data.point))
                }
                result.data.point
            }

            is Result.Failure -> {
                _state.update { it.copy(locationError = result.error) }
                if (result.error == LocationError.PERMISSION_DENIED && requestPermission) {
                    eventChannel.send(DrivingEvent.RequestLocationPermission)
                }
                null
            }
        }
    }

    private fun recenter() {
        val userLocation = _state.value.userLocation
        if (userLocation == null) {
            loadLocation(requestPermission = true)
            return
        }
        viewModelScope.launch { eventChannel.send(DrivingEvent.CenterOn(userLocation)) }
    }

    /** Starts or stops streaming the rider's location to match [DrivingState.isSharingLocation]. */
    private fun updateLocationSharing() {
        val share = _state.value.isSharingLocation
        if (!share) {
            locationJob?.cancel()
            locationJob = null
            return
        }
        if (locationJob?.isActive == true) return
        if (locationProvider.checkAvailability() is Result.Failure) return

        locationJob = viewModelScope.launch {
            locationProvider.observeLocation().collect { location ->
                _state.update {
                    it.copy(
                        userLocation = location.point,
                        userBearing = location.bearingDegrees ?: it.userBearing
                    )
                }
                // The route to the pickup waits for the first fix
                val state = _state.value
                if (state.ride?.status?.isToPickup() == true && state.route == null && routeJob?.isActive != true) {
                    loadRoute()
                }
                riderService.shareLocation(location.point).onFailure { error ->
                    // Presence expired (e.g. after a long time in the background) and no ride
                    if (error == DataError.Remote.CONFLICT && _state.value.ride == null) {
                        setOffline()
                        showMessage(UiText.Resource(Res.string.error_went_offline))
                    }
                }
            }
        }
    }

    // Availability

    private fun loadStatus() {
        _state.update { it.copy(isStatusLoading = true, statusFailed = false) }
        viewModelScope.launch {
            when (val result = riderService.getStatus()) {
                is Result.Success -> {
                    val status = result.data
                    _state.update {
                        it.copy(
                            isStatusLoading = false,
                            vehicle = status.vehicle,
                            isOnline = status.online,
                            requests = if (status.online) it.requests else emptyList(),
                        )
                    }
                    if (status.vehicle == null && !hasPromptedForVehicle) {
                        hasPromptedForVehicle = true
                        eventChannel.send(DrivingEvent.OpenVehicleSetup)
                    }
                    if (status.online) loadOpenRides()
                    updateLocationSharing()
                }

                is Result.Failure -> {
                    _state.update { it.copy(isStatusLoading = false, statusFailed = true) }
                }
            }
        }
    }

    private fun goOnline() {
        if (_state.value.isChangingAvailability) return
        _state.update { it.copy(isChangingAvailability = true) }
        viewModelScope.launch {
            val location = currentLocation(requestPermission = false)
            if (location == null) {
                _state.update { it.copy(isChangingAvailability = false) }
                if (_state.value.locationError == LocationError.PERMISSION_DENIED) {
                    goOnlineAfterPermission = true
                    eventChannel.send(DrivingEvent.RequestLocationPermission)
                }
                return@launch
            }

            val result = riderService.goOnline(location)
            _state.update { it.copy(isChangingAvailability = false) }
            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isOnline = result.data.online,
                            vehicle = result.data.vehicle
                        )
                    }
                    loadOpenRides()
                    updateLocationSharing()
                }

                is Result.Failure -> {
                    if (result.error == DataError.Remote.FORBIDDEN) {
                        // No vehicle on the backend
                        _state.update { it.copy(vehicle = null) }
                        eventChannel.send(DrivingEvent.OpenVehicleSetup)
                    } else {
                        showMessage(result.error.toUiText())
                    }
                }
            }
        }
    }

    private fun goOffline() {
        if (_state.value.isChangingAvailability) return
        _state.update { it.copy(isChangingAvailability = true) }
        viewModelScope.launch {
            val result = riderService.goOffline()
            _state.update { it.copy(isChangingAvailability = false) }
            when (result) {
                is Result.Success -> setOffline()
                is Result.Failure -> showMessage(result.error.toUiText())
            }
        }
    }

    private fun setOffline() {
        _state.update {
            it.copy(
                isOnline = false,
                requests = emptyList(),
                selectedRideId = null,
                offerAmount = null
            )
        }
        updateLocationSharing()
    }

    // Requests and offers

    private fun loadOpenRides() {
        viewModelScope.launch {
            when (val result = rideService.getOpenRides()) {
                is Result.Success -> _state.update { state ->
                    val requests = result.data.sortedForRider()
                    state.copy(requests = requests).keepSelectionIn(requests)
                }

                is Result.Failure -> {
                    // CONFLICT: the backend says we're offline (presence expired)
                    if (result.error == DataError.Remote.CONFLICT) setOffline()
                }
            }
        }
    }

    private fun selectRequest(rideId: String) {
        _state.update { state ->
            if (state.selectedRideId == rideId) {
                state.copy(selectedRideId = null, offerAmount = null)
            } else {
                val request =
                    state.requests.firstOrNull { it.rideId == rideId } ?: return@update state
                state.copy(selectedRideId = rideId, offerAmount = request.offeredFare)
            }
        }
        val request = _state.value.selectedRequest ?: return
        viewModelScope.launch {
            eventChannel.send(
                DrivingEvent.FitPoints(
                    listOfNotNull(
                        _state.value.userLocation,
                        request.pickup.point,
                        request.dropoff.point
                    )
                )
            )
        }
    }

    private fun changeOffer(direction: Int) {
        _state.update { state ->
            val request = state.selectedRequest ?: return@update state
            val amount = state.offerAmount ?: return@update state
            val range = state.offerRange ?: return@update state
            state.copy(offerAmount = (amount + direction * request.fareStep).coerceIn(range))
        }
    }

    private fun submitOffer() {
        val state = _state.value
        val request = state.selectedRequest ?: return
        val amount = state.offerAmount ?: return
        if (state.submittingRideId != null || request.myOffer != null) return

        _state.update { it.copy(submittingRideId = request.rideId) }
        viewModelScope.launch {
            // Null takes the customer's fare as-is (the backend's ACCEPT_FARE)
            val result = rideService.submitOffer(
                rideId = request.rideId,
                amount = amount.takeIf { it != request.offeredFare }
            )
            _state.update { it.copy(submittingRideId = null) }
            when (result) {
                is Result.Success -> {
                    val offer = result.data
                    if (offer.status == OfferStatus.ACCEPTED) {
                        // Auto-accept assigned us straight away
                        syncActiveRide()
                    } else {
                        updateMyOffer(offer)
                    }
                }

                is Result.Failure -> when (result.error) {
                    DataError.Remote.BAD_REQUEST -> showMessage(UiText.Resource(Res.string.error_offer_out_of_range))
                    DataError.Remote.CONFLICT, DataError.Remote.NOT_FOUND -> {
                        removeRequest(request.rideId)
                        showMessage(UiText.Resource(Res.string.error_ride_not_available))
                        // Also covers "already on a ride" and "went offline"
                        syncActiveRide()
                        loadStatus()
                    }

                    else -> showMessage(result.error.toUiText())
                }
            }
        }
    }

    private fun withdrawOffer(rideId: String, offerId: String) {
        if (_state.value.withdrawingOfferId != null) return
        _state.update { it.copy(withdrawingOfferId = offerId) }
        viewModelScope.launch {
            val result = rideService.withdrawOffer(rideId, offerId)
            _state.update { it.copy(withdrawingOfferId = null) }
            when (result) {
                is Result.Success -> clearMyOffer(rideId)
                is Result.Failure -> {
                    // NOT_FOUND: it was already accepted, declined or expired; the events tell which
                    if (result.error != DataError.Remote.NOT_FOUND) showMessage(result.error.toUiText())
                    else clearMyOffer(rideId)
                }
            }
        }
    }

    private fun updateMyOffer(offer: RideOffer) {
        _state.update { state ->
            state.copy(requests = state.requests.map { request ->
                if (request.rideId == offer.rideId && request.searchRound == offer.searchRound) {
                    request.copy(myOffer = offer)
                } else {
                    request
                }
            })
        }
    }

    private fun clearMyOffer(rideId: String) {
        _state.update { state ->
            state.copy(requests = state.requests.map { request ->
                if (request.rideId == rideId) request.copy(myOffer = null) else request
            })
        }
    }

    private fun removeRequest(rideId: String) {
        _state.update { state ->
            val requests = state.requests.filterNot { it.rideId == rideId }
            state.copy(requests = requests).keepSelectionIn(requests)
        }
    }

    // Trip

    private fun tripStep(step: suspend (rideId: String) -> Result<Ride, DataError.Remote>) {
        val ride = _state.value.ride ?: return
        if (_state.value.isUpdatingTrip) return

        _state.update { it.copy(isUpdatingTrip = true) }
        viewModelScope.launch {
            val result = step(ride.id)
            _state.update { it.copy(isUpdatingTrip = false) }
            when (result) {
                is Result.Success -> applyRide(result.data)
                is Result.Failure -> onRideActionFailed(result.error)
            }
        }
    }

    private fun cancelRide() {
        val ride = _state.value.ride ?: return
        if (_state.value.isCancelling) return

        _state.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val result = rideService.cancelRide(ride.id)
            _state.update { it.copy(isCancelling = false) }
            when (result) {
                is Result.Success -> applyRide(result.data)
                is Result.Failure -> onRideActionFailed(result.error)
            }
        }
    }

    private fun dismissFinished() {
        _state.update { it.copy(finishedRide = null) }
        _state.value.userLocation?.let { location ->
            viewModelScope.launch { eventChannel.send(DrivingEvent.CenterOn(location)) }
        }
        if (_state.value.isOnline) loadOpenRides()
    }

    private suspend fun onRideActionFailed(error: DataError.Remote) {
        when (error) {
            DataError.Remote.CONFLICT, DataError.Remote.NOT_FOUND -> {
                showMessage(UiText.Resource(Res.string.error_ride_changed))
                syncActiveRide()
            }

            else -> showMessage(error.toUiText())
        }
    }

    // Server state

    private fun observeRideEvents() {
        viewModelScope.launch {
            rideEventSource.events.collect { event -> onRideEvent(event) }
        }
    }

    /** Events missed while disconnected are not replayed, so resync on every (re)connect. */
    private fun observeConnection() {
        viewModelScope.launch {
            rideEventSource.connectionState
                .map { it == ConnectionState.CONNECTED }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    syncActiveRide()
                    loadStatus()
                }
        }
    }

    private fun onRideEvent(event: RideEvent) {
        when (event) {
            is RideEvent.RideRequested -> onRideRequested(event.ride)
            is RideEvent.RideUnavailable -> removeRequest(event.rideId)
            is RideEvent.OfferUpdated -> onOfferUpdated(event.offer)
            is RideEvent.RideUpdated -> {
                val current = _state.value.ride
                if (current == null || current.id == event.ride.id) applyRide(event.ride)
            }

            // Customer-only messages
            is RideEvent.OfferReceived,
            is RideEvent.SearchRoundEnded,
            is RideEvent.RiderLocation -> Unit
        }
    }

    /** A new request, or a known one re-priced into a new round (any earlier offer expired). */
    private fun onRideRequested(request: OpenRide) {
        _state.update { state ->
            if (!state.isOnline || state.ride != null) return@update state
            val previous = state.requests.firstOrNull { it.rideId == request.rideId }
            val requests =
                (state.requests.filterNot { it.rideId == request.rideId } + request).sortedForRider()
            val repriced = previous != null && previous.searchRound != request.searchRound
            state.copy(
                requests = requests,
                offerAmount = if (repriced && state.selectedRideId == request.rideId) request.offeredFare else state.offerAmount
            )
        }
    }

    private fun onOfferUpdated(offer: RideOffer) {
        when (offer.status) {
            OfferStatus.PENDING -> updateMyOffer(offer)
            // The customer chose someone else for this ride, or declined us; we can't offer again this round
            OfferStatus.DECLINED -> removeRequest(offer.rideId)
            OfferStatus.EXPIRED, OfferStatus.WITHDRAWN -> clearMyOffer(offer.rideId)
            // RIDE_UPDATED follows with the ride; resync in case it was missed
            OfferStatus.ACCEPTED -> viewModelScope.launch { syncActiveRide() }
        }
    }

    private suspend fun syncActiveRide() {
        val previous = _state.value.ride
        when (val result = rideService.getActiveRide()) {
            is Result.Success -> {
                val ride = result.data
                when {
                    ride != null -> applyRide(ride)
                    // Ended while we weren't listening: fetch it to show how
                    previous != null -> rideService.getRide(previous.id).onSuccess { applyRide(it) }
                }
            }

            is Result.Failure -> Unit
        }
    }

    private fun applyRide(ride: Ride) {
        val previous = _state.value.ride
        if (!ride.status.isActive) {
            if (previous?.id != ride.id) return
            routeJob?.cancel()
            _state.update { it.copy(ride = null, route = null, finishedRide = ride) }
            updateLocationSharing()
            return
        }

        _state.update {
            it.copy(
                ride = ride,
                requests = emptyList(),
                selectedRideId = null,
                offerAmount = null,
                finishedRide = null,
            )
        }
        val legChanged =
            previous?.id != ride.id || previous.status.isToPickup() != ride.status.isToPickup()
        if (legChanged) loadRoute()
        updateLocationSharing()
    }

    /** The driving leg for the map: from the rider to the pickup, then the trip itself. */
    private fun loadRoute() {
        val state = _state.value
        val ride = state.ride ?: return
        val from = if (ride.status.isToPickup()) state.userLocation ?: return else ride.pickup.point
        val to = state.nextStop ?: return

        routeJob?.cancel()
        _state.update { it.copy(route = null) }
        routeJob = viewModelScope.launch {
            val route = (routeService.getRoute(from, to) as? Result.Success)?.data
            _state.update { it.copy(route = route) }
            eventChannel.send(DrivingEvent.FitPoints(route?.points ?: listOf(from, to)))
        }
    }

    private suspend fun showMessage(message: UiText) {
        eventChannel.send(DrivingEvent.ShowMessage(message))
    }

    private fun RideStatus.isToPickup(): Boolean =
        this == RideStatus.ACCEPTED || this == RideStatus.ARRIVED

    /** Nearest pickup first; unknown distances last. */
    private fun List<OpenRide>.sortedForRider(): List<OpenRide> =
        sortedBy { it.pickupDistanceMeters ?: Int.MAX_VALUE }

    private fun DrivingState.keepSelectionIn(requests: List<OpenRide>): DrivingState =
        if (requests.any { it.rideId == selectedRideId }) this else copy(
            selectedRideId = null,
            offerAmount = null
        )
}
