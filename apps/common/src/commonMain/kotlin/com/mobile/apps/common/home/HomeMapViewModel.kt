package com.mobile.apps.common.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.location.GeoPoint
import com.mobile.core.domain.location.LocationError
import com.mobile.core.domain.location.LocationProvider
import com.mobile.core.domain.place.Place
import com.mobile.core.domain.place.PlaceSearchService
import com.mobile.core.domain.route.RouteService
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.presentation.permissions.PermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Temporary home for both apps: exercises the Phase 5 map, location, reverse geocoding and
 * routing on a device until the Phase 6/7 ride flows replace it.
 */
class HomeMapViewModel(
    private val locationProvider: LocationProvider,
    private val placeSearchService: PlaceSearchService,
    private val routeService: RouteService,
) : ViewModel() {

    private val eventChannel = Channel<HomeMapEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false
    private var reverseGeocodeJob: Job? = null
    private var routeJob: Job? = null

    private val _state = MutableStateFlow(HomeMapState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadLocation(requestPermission = true)
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: HomeMapAction) {
        when (action) {
            is HomeMapAction.OnLocationPermissionResult -> {
                if (action.state == PermissionState.GRANTED) {
                    loadLocation(requestPermission = false)
                } else {
                    _state.update { it.copy(locationError = LocationError.PERMISSION_DENIED) }
                }
            }

            is HomeMapAction.OnCameraIdle -> onCameraIdle(action.center)
            is HomeMapAction.OnMapClick -> setDestination(action.point)
            HomeMapAction.OnClearDestinationClick -> clearDestination()
            HomeMapAction.OnRecenterClick -> recenter()
            HomeMapAction.OnLogoutClick -> Unit
        }
    }

    private fun loadLocation(requestPermission: Boolean) {
        viewModelScope.launch {
            locationProvider.getCurrentLocation()
                .onSuccess { location ->
                    _state.update { it.copy(userLocation = location.point, locationError = null) }
                    if (_state.value.destination == null) {
                        eventChannel.send(HomeMapEvent.CenterOn(location.point))
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(locationError = error) }
                    if (error == LocationError.PERMISSION_DENIED && requestPermission) {
                        eventChannel.send(HomeMapEvent.RequestLocationPermission)
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
            eventChannel.send(HomeMapEvent.CenterOn(userLocation))
        }
    }

    /** While no destination is set, the pin in the middle of the map is the pickup. */
    private fun onCameraIdle(center: GeoPoint) {
        if (_state.value.destination != null || center == _state.value.pickup) return

        _state.update { it.copy(pickup = center, isResolvingPickup = true) }
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            val address = placeSearchService.reverseGeocode(center).placeLabel()
            _state.update { it.copy(pickupAddress = address, isResolvingPickup = false) }
        }
    }

    private fun setDestination(point: GeoPoint) {
        val pickup = _state.value.pickup ?: return

        _state.update {
            it.copy(
                destination = Place(id = "map", name = "Dropped pin", address = "", point = point),
                route = null,
                isLoadingRoute = true,
                routeFailed = false
            )
        }
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            launch {
                placeSearchService.reverseGeocode(point).onSuccess { place ->
                    if (place != null) {
                        _state.update { it.copy(destination = place.copy(point = point)) }
                    }
                }
            }
            when (val result = routeService.getRoute(pickup, point)) {
                is Result.Success -> {
                    _state.update { it.copy(route = result.data, isLoadingRoute = false) }
                    eventChannel.send(HomeMapEvent.ShowRoute(result.data.points))
                }

                is Result.Failure -> {
                    _state.update { it.copy(isLoadingRoute = false, routeFailed = true) }
                    eventChannel.send(HomeMapEvent.ShowRoute(listOf(pickup, point)))
                }
            }
        }
    }

    private fun clearDestination() {
        routeJob?.cancel()
        _state.update {
            it.copy(destination = null, route = null, isLoadingRoute = false, routeFailed = false)
        }
        _state.value.pickup?.let { pickup ->
            viewModelScope.launch { eventChannel.send(HomeMapEvent.CenterOn(pickup)) }
        }
    }

    private fun Result<Place?, *>.placeLabel(): String? = when (this) {
        is Result.Success -> data?.let { place ->
            listOf(place.name, place.address).filter { it.isNotBlank() }.joinToString(", ")
        }

        is Result.Failure -> null
    }
}
