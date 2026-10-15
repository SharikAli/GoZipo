@file:OptIn(FlowPreview::class)

package com.mobile.booking.presentation.booking

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.ride.presentation.util.distanceMetersTo
import com.mobile.booking.presentation.util.label
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.place.PlaceSearchService
import com.mobile.core.domain.realtime.ConnectionState
import com.mobile.core.domain.route.RouteService
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.presentation.permissions.PermissionState
import com.mobile.core.presentation.util.UiText
import com.mobile.core.presentation.util.toUiText
import com.mobile.ride.domain.CancelledBy
import com.mobile.ride.domain.OfferStatus
import com.mobile.ride.domain.Ride
import com.mobile.ride.domain.RideEvent
import com.mobile.ride.domain.RideEventSource
import com.mobile.ride.domain.RideOffer
import com.mobile.ride.domain.RideRequest
import com.mobile.ride.domain.RideService
import com.mobile.ride.domain.RideStatus
import com.mobile.ride.domain.RideStop
import com.mobile.ride.domain.VehicleType
import gozipo.feature.booking.presentation.generated.resources.Res
import gozipo.feature.booking.presentation.generated.resources.error_active_ride_exists
import gozipo.feature.booking.presentation.generated.resources.error_fare_out_of_range
import gozipo.feature.booking.presentation.generated.resources.error_ride_changed
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

/**
 * The customer's whole ride on one map screen: choose stops, pick a vehicle and fare, collect
 * offers, then follow the trip. The backend owns ride state; this mirrors it from REST
 * responses and realtime events, and resyncs whenever the socket (re)connects.
 */
class BookingViewModel(
    private val locationProvider: LocationProvider,
    private val placeSearchService: PlaceSearchService,
    private val routeService: RouteService,
    private val rideService: RideService,
    private val rideEventSource: RideEventSource,
) : ViewModel() {

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 1000L
        const val MIN_QUERY_LENGTH = 2

        /** Camera settling this close to the pickup doesn't move it (keeps a searched address). */
        const val PICKUP_TOLERANCE_METERS = 5.0

        /** The backend's limit for stop addresses. */
        const val MAX_ADDRESS_LENGTH = 255
    }

    private val eventChannel = Channel<BookingEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false
    private var reverseGeocodeJob: Job? = null
    private var stopsJob: Job? = null
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(BookingState())
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

    fun onAction(action: BookingAction) {
        when (action) {
            is BookingAction.OnLocationPermissionResult -> {
                if (action.state == PermissionState.GRANTED) {
                    loadLocation(requestPermission = false)
                } else {
                    _state.update { it.copy(locationError = LocationError.PERMISSION_DENIED) }
                }
            }

            is BookingAction.OnCameraIdle -> onCameraIdle(action.center)
            is BookingAction.OnMapClick -> onMapClick(action.point)
            BookingAction.OnRecenterClick -> recenter()
            BookingAction.OnLogoutClick -> Unit

            is BookingAction.OnOpenSearch -> openSearch(action.target)
            BookingAction.OnCloseSearch -> closeSearch()
            is BookingAction.OnPlaceSelected -> onPlaceSelected(action.place)
            BookingAction.OnChooseOnMapClick -> chooseOnMap()
            BookingAction.OnClearDropoffClick -> clearDropoff()
            BookingAction.OnConfirmDestinationPinClick -> confirmDestinationPin()
            BookingAction.OnCancelDestinationPinClick -> cancelDestinationPin()

            is BookingAction.OnVehicleSelected -> selectVehicle(action.vehicleType)
            BookingAction.OnRetryEstimatesClick -> loadStops()
            BookingAction.OnDecreaseFareClick -> changeFare(-1)
            BookingAction.OnIncreaseFareClick -> changeFare(+1)
            is BookingAction.OnAutoAcceptChange -> _state.update { it.copy(autoAccept = action.enabled) }
            BookingAction.OnFindOffersClick -> requestRide()

            BookingAction.OnConfirmFareClick -> confirmFare()
            is BookingAction.OnAcceptOfferClick -> acceptOffer(action.offerId)
            is BookingAction.OnDeclineOfferClick -> declineOffer(action.offerId)
            BookingAction.OnCancelRideClick -> cancelRide()

            BookingAction.OnFinishedDismiss -> dismissFinished()
        }
    }

    // Location and pickup

    private fun loadLocation(requestPermission: Boolean) {
        viewModelScope.launch {
            locationProvider.getCurrentLocation()
                .onSuccess { location ->
                    _state.update {
                        it.copy(
                            userLocation = location.point,
                            locationError = null
                        )
                    }
                    if (_state.value.stage == BookingStage.CHOOSE_STOPS) {
                        eventChannel.send(BookingEvent.CenterOn(location.point))
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(locationError = error) }
                    if (error == LocationError.PERMISSION_DENIED && requestPermission) {
                        eventChannel.send(BookingEvent.RequestLocationPermission)
                    }
                }
        }
    }

    private fun recenter() {
        val userLocation = _state.value.userLocation
        if (userLocation == null) {
            loadLocation(requestPermission = true)
            return
        }
        viewModelScope.launch {
            eventChannel.send(BookingEvent.CenterOn(userLocation))
        }
    }

    /**
     * The pin in the middle of the map is the pickup while choosing stops, or the destination
     * after "Choose on map" in the destination search.
     */
    private fun onCameraIdle(center: GeoPoint) {
        val state = _state.value
        if (state.stage == BookingStage.CHOOSE_DESTINATION_ON_MAP) {
            moveDestinationPin(center)
            return
        }
        if (state.stage != BookingStage.CHOOSE_STOPS) return
        val pickup = state.pickup
        if (pickup != null && pickup.distanceMetersTo(center) < PICKUP_TOLERANCE_METERS) return

        _state.update { it.copy(pickup = center, pickupAddress = null, isResolvingPickup = true) }
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            val address = when (val result = placeSearchService.reverseGeocode(center)) {
                is Result.Success -> result.data?.label()
                is Result.Failure -> null
            }
            _state.update { it.copy(pickupAddress = address, isResolvingPickup = false) }
        }
    }

    private fun onMapClick(point: GeoPoint) {
        val stage = _state.value.stage
        if (stage != BookingStage.CHOOSE_STOPS &&
            stage != BookingStage.CHOOSE_VEHICLE &&
            stage != BookingStage.CHOOSE_DESTINATION_ON_MAP
        ) return
        if (_state.value.pickup == null) return

        reverseGeocodeJob?.cancel()
        _state.update { it.copy(destinationPin = null) }
        setDropoff(Place(id = "map", name = "", address = "", point = point))
        viewModelScope.launch {
            placeSearchService.reverseGeocode(point).onSuccess { place ->
                if (place != null && _state.value.dropoff?.point == point) {
                    _state.update { it.copy(dropoff = place.copy(point = point)) }
                }
            }
        }
    }

    // Place search

    private fun openSearch(target: SearchTarget) {
        val search = PlaceSearch(target = target)
        _state.update { it.copy(search = search) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            snapshotFlow { search.query.text.toString().trim() }
                .distinctUntilChanged()
                .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)
                .collect { query -> searchPlaces(query) }
        }
    }

    private suspend fun searchPlaces(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            updateSearch { it.copy(results = emptyList(), isSearching = false, error = null) }
            return
        }
        updateSearch { it.copy(isSearching = true, error = null) }
        val near = _state.value.userLocation ?: _state.value.pickup
        when (val result = placeSearchService.search(query, near)) {
            is Result.Success -> updateSearch {
                it.copy(
                    results = result.data,
                    isSearching = false
                )
            }

            is Result.Failure -> updateSearch {
                it.copy(
                    isSearching = false,
                    error = result.error.toUiText()
                )
            }
        }
    }

    private fun updateSearch(transform: (PlaceSearch) -> PlaceSearch) {
        _state.update { state -> state.copy(search = state.search?.let(transform)) }
    }

    private fun closeSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = null) }
    }

    private fun onPlaceSelected(place: Place) {
        val target = _state.value.search?.target ?: return
        closeSearch()
        when (target) {
            SearchTarget.PICKUP -> {
                reverseGeocodeJob?.cancel()
                _state.update {
                    it.copy(
                        pickup = place.point,
                        pickupAddress = place.label(),
                        isResolvingPickup = false
                    )
                }
                if (_state.value.dropoff != null) {
                    loadStops()
                } else {
                    viewModelScope.launch { eventChannel.send(BookingEvent.CenterOn(place.point)) }
                }
            }

            SearchTarget.DROPOFF -> setDropoff(place)
        }
    }

    private fun chooseOnMap() {
        val target = _state.value.search?.target ?: return
        closeSearch()
        when (target) {
            // The pickup pin only exists while choosing stops
            SearchTarget.PICKUP -> clearDropoff()
            SearchTarget.DROPOFF -> {
                // Starts at the current destination when changing it, otherwise waits for the map to move
                val current = _state.value.dropoff
                _state.update {
                    it.copy(
                        destinationPin = DestinationPin(
                            point = current?.point,
                            address = current?.label()?.takeIf(String::isNotBlank)
                        )
                    )
                }
                val center = current?.point ?: _state.value.pickup
                center?.let { viewModelScope.launch { eventChannel.send(BookingEvent.CenterOn(it)) } }
            }
        }
    }

    // Destination on the map

    private fun moveDestinationPin(center: GeoPoint) {
        val pin = _state.value.destinationPin ?: return
        val current = pin.point
        if (current != null && current.distanceMetersTo(center) < PICKUP_TOLERANCE_METERS) return
        // The camera settling on the pickup when this step opens isn't a choice yet
        val pickup = _state.value.pickup
        if (current == null && pickup != null && pickup.distanceMetersTo(center) < PICKUP_TOLERANCE_METERS) return

        _state.update {
            it.copy(
                destinationPin = DestinationPin(
                    point = center,
                    isResolving = true
                )
            )
        }
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            val address = when (val result = placeSearchService.reverseGeocode(center)) {
                is Result.Success -> result.data?.label()
                is Result.Failure -> null
            }
            _state.update { state ->
                val latest = state.destinationPin
                if (latest?.point != center) return@update state
                state.copy(destinationPin = latest.copy(address = address, isResolving = false))
            }
        }
    }

    private fun confirmDestinationPin() {
        val pin = _state.value.destinationPin ?: return
        val point = pin.point ?: return
        if (pin.isResolving) return
        _state.update { it.copy(destinationPin = null) }
        setDropoff(Place(id = "map", name = pin.address.orEmpty(), address = "", point = point))
    }

    private fun cancelDestinationPin() {
        reverseGeocodeJob?.cancel()
        _state.update { it.copy(destinationPin = null) }
        val state = _state.value
        viewModelScope.launch {
            when {
                // Back to the trip that was already set
                state.dropoff != null -> eventChannel.send(
                    BookingEvent.FitPoints(
                        state.route?.points ?: listOfNotNull(
                            state.pickup,
                            state.dropoff.point
                        )
                    )
                )

                state.pickup != null -> eventChannel.send(BookingEvent.CenterOn(state.pickup))
            }
        }
    }

    // Stops, route and fares

    private fun setDropoff(place: Place) {
        _state.update { it.copy(dropoff = place) }
        loadStops()
    }

    private fun clearDropoff() {
        stopsJob?.cancel()
        _state.update {
            it.copy(
                dropoff = null,
                route = null,
                isLoadingRoute = false,
                estimates = emptyList(),
                isLoadingEstimates = false,
                estimatesFailed = false,
                selectedVehicle = null,
                offeredFare = null,
            )
        }
        _state.value.pickup?.let { pickup ->
            viewModelScope.launch { eventChannel.send(BookingEvent.CenterOn(pickup)) }
        }
    }

    /** Route for the map and fares for every vehicle, for the current pickup and drop-off. */
    private fun loadStops() {
        val pickup = _state.value.pickup ?: return
        val dropoff = _state.value.dropoff?.point ?: return

        stopsJob?.cancel()
        _state.update {
            it.copy(
                route = null,
                isLoadingRoute = true,
                isLoadingEstimates = true,
                estimatesFailed = false
            )
        }
        stopsJob = viewModelScope.launch {
            launch {
                val route = (routeService.getRoute(pickup, dropoff) as? Result.Success)?.data
                _state.update { it.copy(route = route, isLoadingRoute = false) }
                eventChannel.send(BookingEvent.FitPoints(route?.points ?: listOf(pickup, dropoff)))
            }
            rideService.estimateFares(pickup, dropoff)
                .onSuccess { estimates ->
                    _state.update { state ->
                        val selected =
                            state.selectedVehicle?.takeIf { type -> estimates.any { it.vehicleType == type } }
                                ?: estimates.firstOrNull { it.vehicleType == VehicleType.MINI }?.vehicleType
                                ?: estimates.firstOrNull()?.vehicleType
                        state.copy(
                            estimates = estimates,
                            isLoadingEstimates = false,
                            selectedVehicle = selected,
                            offeredFare = estimates.firstOrNull { it.vehicleType == selected }?.recommendedFare
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingEstimates = false, estimatesFailed = true) }
                }
        }
    }

    private fun selectVehicle(vehicleType: VehicleType) {
        _state.update { state ->
            if (state.selectedVehicle == vehicleType) return@update state
            state.copy(
                selectedVehicle = vehicleType,
                offeredFare = state.estimates.firstOrNull { it.vehicleType == vehicleType }?.recommendedFare
            )
        }
    }

    private fun changeFare(direction: Int) {
        _state.update { state ->
            val fare = state.offeredFare ?: return@update state
            val range = state.fareRange ?: return@update state
            state.copy(offeredFare = (fare + direction * state.fareStep).coerceIn(range))
        }
    }

    // Ride

    private fun requestRide() {
        val state = _state.value
        val pickup = state.pickup ?: return
        val dropoff = state.dropoff ?: return
        val vehicleType = state.selectedVehicle ?: return
        val fare = state.offeredFare ?: return
        if (state.isRequesting) return

        _state.update { it.copy(isRequesting = true) }
        viewModelScope.launch {
            val request = RideRequest(
                pickup = RideStop(pickup, addressOf(state.pickupAddress, pickup)),
                dropoff = RideStop(dropoff.point, addressOf(dropoff.label(), dropoff.point)),
                vehicleType = vehicleType,
                offeredFare = fare,
                autoAccept = state.autoAccept
            )
            when (val result = rideService.requestRide(request)) {
                is Result.Success -> {
                    _state.update { it.copy(isRequesting = false) }
                    applyRide(result.data)
                    eventChannel.send(BookingEvent.CenterOn(pickup))
                }

                is Result.Failure -> {
                    _state.update { it.copy(isRequesting = false) }
                    when (result.error) {
                        DataError.Remote.CONFLICT -> {
                            showMessage(UiText.Resource(Res.string.error_active_ride_exists))
                            syncActiveRide()
                        }

                        DataError.Remote.BAD_REQUEST -> {
                            showMessage(UiText.Resource(Res.string.error_fare_out_of_range))
                            loadStops()
                        }

                        else -> showMessage(result.error.toUiText())
                    }
                }
            }
        }
    }

    /** Raise the fare while searching, or retry at the same fare once the round ended. */
    private fun confirmFare() {
        val state = _state.value
        val ride = state.ride ?: return
        val fare = state.offeredFare ?: return
        if (state.isUpdatingFare || !state.canConfirmFare) return

        _state.update { it.copy(isUpdatingFare = true) }
        viewModelScope.launch {
            val result = rideService.updateFare(ride.id, fare)
            _state.update { it.copy(isUpdatingFare = false) }
            when (result) {
                is Result.Success -> applyRide(result.data)
                is Result.Failure -> onRideActionFailed(result.error)
            }
        }
    }

    private fun acceptOffer(offerId: String) {
        val ride = _state.value.ride ?: return
        if (_state.value.acceptingOfferId != null) return

        _state.update { it.copy(acceptingOfferId = offerId) }
        viewModelScope.launch {
            val result = rideService.acceptOffer(ride.id, offerId)
            _state.update { it.copy(acceptingOfferId = null) }
            when (result) {
                is Result.Success -> applyRide(result.data)
                is Result.Failure -> {
                    removeOffer(offerId)
                    onRideActionFailed(result.error)
                }
            }
        }
    }

    private fun declineOffer(offerId: String) {
        val ride = _state.value.ride ?: return
        removeOffer(offerId)
        viewModelScope.launch {
            // Nothing to undo: a declined or vanished offer is gone either way
            rideService.declineOffer(ride.id, offerId)
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
        val finished = _state.value.finishedRide ?: return
        if (finished.status == RideStatus.COMPLETED) {
            _state.update {
                BookingState(locationError = it.locationError, userLocation = it.userLocation)
            }
            _state.value.userLocation?.let { location ->
                viewModelScope.launch { eventChannel.send(BookingEvent.CenterOn(location)) }
            }
        } else {
            // Cancelled by the rider or the system: offer the same trip again
            backToVehicleChoice()
        }
    }

    private suspend fun onRideActionFailed(error: DataError.Remote) {
        when (error) {
            DataError.Remote.CONFLICT, DataError.Remote.NOT_FOUND -> {
                showMessage(UiText.Resource(Res.string.error_ride_changed))
                syncActiveRide()
            }

            DataError.Remote.BAD_REQUEST -> {
                showMessage(UiText.Resource(Res.string.error_fare_out_of_range))
            }

            else -> {
                showMessage(error.toUiText())
            }
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
                .collect { syncActiveRide() }
        }
    }

    private fun onRideEvent(event: RideEvent) {
        val current = _state.value.ride
        when (event) {
            is RideEvent.RideUpdated -> {
                if (current == null || current.id == event.ride.id) applyRide(event.ride)
            }

            is RideEvent.SearchRoundEnded -> {
                if (current?.id == event.ride.id) applyRide(event.ride)
            }

            is RideEvent.OfferReceived -> upsertOffer(event.offer)
            is RideEvent.OfferUpdated -> upsertOffer(event.offer)
            is RideEvent.RiderLocation -> {
                if (current?.id == event.rideId) {
                    _state.update { it.copy(riderLocation = event.point) }
                }
            }

            is RideEvent.RideRequested, is RideEvent.RideUnavailable -> Unit
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
            onRideEnded(ride)
            return
        }

        _state.update { state ->
            val sameRound = previous?.id == ride.id && previous.searchRound == ride.searchRound
            state.copy(
                ride = ride,
                // Keep the stops on screen when the ride was restored after a restart
                pickup = ride.pickup.point,
                pickupAddress = state.pickupAddress.takeIf { previous != null }
                    ?: ride.pickup.address,
                dropoff = state.dropoff.takeIf { previous != null }
                    ?: Place(
                        id = "ride",
                        name = ride.dropoff.address,
                        address = "",
                        point = ride.dropoff.point
                    ),
                offers = if (ride.status == RideStatus.SEARCHING && sameRound) state.offers else emptyList(),
                offeredFare = if (ride.status == RideStatus.SEARCHING && !sameRound) ride.offeredFare else state.offeredFare,
                riderLocation = if (ride.status == RideStatus.SEARCHING) null else state.riderLocation,
            )
        }

        if (ride.status == RideStatus.SEARCHING && previous?.searchRound != ride.searchRound) {
            loadOffers(ride.id)
        }
        if (_state.value.estimates.none { it.vehicleType == ride.vehicleType }) {
            loadEstimatesFor(ride)
        }
        if (_state.value.route == null && !_state.value.isLoadingRoute) {
            loadRouteFor(ride)
        }
    }

    private fun onRideEnded(ride: Ride) {
        if (ride.status == RideStatus.CANCELLED && ride.cancelledBy == CancelledBy.CUSTOMER) {
            backToVehicleChoice()
            return
        }
        _state.update {
            it.copy(
                ride = null,
                offers = emptyList(),
                riderLocation = null,
                finishedRide = ride,
            )
        }
    }

    /** Back to choosing a vehicle for the same stops (after a cancelled ride). */
    private fun backToVehicleChoice() {
        _state.update {
            it.copy(
                ride = null,
                offers = emptyList(),
                riderLocation = null,
                finishedRide = null,
                acceptingOfferId = null,
            )
        }
        val state = _state.value
        val selected = state.selectedEstimate
        if (selected != null) {
            _state.update { it.copy(offeredFare = selected.recommendedFare) }
        } else {
            loadStops()
        }
        state.route?.let { route ->
            viewModelScope.launch { eventChannel.send(BookingEvent.FitPoints(route.points)) }
        }
    }

    private fun loadOffers(rideId: String) {
        viewModelScope.launch {
            rideService.getPendingOffers(rideId).onSuccess { offers ->
                if (_state.value.ride?.id == rideId) {
                    _state.update { it.copy(offers = offers.sortedBy(RideOffer::amount)) }
                }
            }
        }
    }

    /** Fare bounds for raising the fare of a ride restored after a restart. */
    private fun loadEstimatesFor(ride: Ride) {
        viewModelScope.launch {
            rideService.estimateFares(ride.pickup.point, ride.dropoff.point)
                .onSuccess { estimates ->
                    _state.update {
                        it.copy(
                            estimates = estimates,
                            selectedVehicle = it.selectedVehicle ?: ride.vehicleType
                        )
                    }
                }
        }
    }

    private fun loadRouteFor(ride: Ride) {
        _state.update { it.copy(isLoadingRoute = true) }
        viewModelScope.launch {
            val route = (routeService.getRoute(
                ride.pickup.point,
                ride.dropoff.point
            ) as? Result.Success)?.data
            _state.update { it.copy(route = route, isLoadingRoute = false) }
        }
    }

    private fun upsertOffer(offer: RideOffer) {
        _state.update { state ->
            val ride = state.ride ?: return@update state
            if (offer.rideId != ride.id || offer.searchRound != ride.searchRound) return@update state
            val others = state.offers.filterNot { it.id == offer.id }
            val offers = if (offer.status == OfferStatus.PENDING) others + offer else others
            state.copy(offers = offers.sortedBy(RideOffer::amount))
        }
    }

    private fun removeOffer(offerId: String) {
        _state.update { state -> state.copy(offers = state.offers.filterNot { it.id == offerId }) }
    }

    private suspend fun showMessage(message: UiText) {
        eventChannel.send(BookingEvent.ShowMessage(message))
    }

    /** The backend needs a non-blank address; fall back to the coordinates. */
    private fun addressOf(label: String?, point: GeoPoint): String {
        val address = label?.takeIf { it.isNotBlank() }
            ?: "${point.latitude.roundTo5()}, ${point.longitude.roundTo5()}"
        return address.take(MAX_ADDRESS_LENGTH)
    }

    private fun Double.roundTo5(): String {
        val scaled = round(this * 100_000) / 100_000
        return scaled.toString()
    }
}
