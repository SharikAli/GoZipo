package com.mobile.driving.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.util.Result
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.presentation.util.toUiText
import com.mobile.ride.domain.RideService
import com.mobile.ride.domain.RiderService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rider's earnings (summed by the backend) and finished rides, newest first. */
class RideHistoryViewModel(
    private val rideService: RideService,
    private val riderService: RiderService,
) : ViewModel() {

    private companion object {
        /** The backend's page size; a shorter page is the last one. */
        const val PAGE_SIZE = 20
    }

    private var hasLoadedInitialData = false
    private var nextPage = 0
    private var pageJob: Job? = null

    private val _state = MutableStateFlow(RideHistoryState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                reload()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: RideHistoryAction) {
        when (action) {
            RideHistoryAction.OnRetryClick -> reload()
            RideHistoryAction.OnLoadMore -> loadNextPage()
            RideHistoryAction.OnBackClick -> Unit
        }
    }

    private fun reload() {
        pageJob?.cancel()
        nextPage = 0
        _state.update { RideHistoryState(earnings = it.earnings) }
        viewModelScope.launch {
            riderService.getEarnings().onSuccess { earnings ->
                _state.update { it.copy(earnings = earnings) }
            }
        }
        loadNextPage()
    }

    private fun loadNextPage() {
        if (_state.value.endReached || pageJob?.isActive == true) return

        val page = nextPage
        _state.update { it.copy(isLoadingMore = page > 0, errorText = null) }
        pageJob = viewModelScope.launch {
            when (val result = rideService.getHistory(page)) {
                is Result.Success -> {
                    nextPage = page + 1
                    _state.update {
                        it.copy(
                            rides = it.rides + result.data,
                            isLoading = false,
                            isLoadingMore = false,
                            endReached = result.data.size < PAGE_SIZE
                        )
                    }
                }

                is Result.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorText = result.error.toUiText()
                    )
                }
            }
        }
    }
}
