package com.mobile.driving.presentation.vehicle

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.core.domain.util.DataError
import com.mobile.core.domain.util.onFailure
import com.mobile.core.domain.util.onSuccess
import com.mobile.core.presentation.util.UiText
import com.mobile.core.presentation.util.toUiText
import com.mobile.ride.domain.RiderService
import com.mobile.ride.domain.Vehicle
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.error_plate_taken
import gozipo.feature.driving.presentation.generated.resources.error_vehicle_invalid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Adds or edits the rider's vehicle (`PUT /riders/me/vehicle`). */
class VehicleViewModel(
    private val riderService: RiderService,
) : ViewModel() {

    private val _events = MutableSharedFlow<VehicleEvent>()
    val events = _events.asSharedFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(VehicleState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadVehicle()
                observeFields()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value
        )

    fun onAction(action: VehicleAction) {
        when (action) {
            is VehicleAction.OnVehicleTypeClick -> _state.update {
                it.copy(vehicleType = action.vehicleType, errorText = null)
            }

            VehicleAction.OnSaveClick -> save()
            VehicleAction.OnBackClick -> Unit
        }
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            riderService.getStatus()
                .onSuccess { status ->
                    val vehicle = status.vehicle
                    if (vehicle != null) {
                        _state.value.apply {
                            makeTextFieldState.setText(vehicle.make)
                            modelTextFieldState.setText(vehicle.model)
                            colorTextFieldState.setText(vehicle.color)
                            plateTextFieldState.setText(vehicle.plateNumber)
                        }
                    }
                    _state.update {
                        it.copy(
                            vehicleType = vehicle?.type,
                            isEditing = vehicle != null,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    // Still usable: saving creates or replaces the vehicle
                    _state.update { it.copy(isLoading = false, errorText = error.toUiText()) }
                }
        }
    }

    private fun observeFields() {
        val state = _state.value
        viewModelScope.launch {
            combine(
                state.makeTextFieldState.textFlow(),
                state.modelTextFieldState.textFlow(),
                state.colorTextFieldState.textFlow(),
                state.plateTextFieldState.textFlow(),
            ) { fields ->
                fields.all { it.isNotBlank() }
            }.collect { filled -> _state.update { it.copy(areFieldsFilled = filled) } }
        }
    }

    private fun save() {
        val state = _state.value
        val vehicleType = state.vehicleType ?: return
        if (!state.canSave) return

        _state.update { it.copy(isSaving = true, errorText = null) }
        viewModelScope.launch {
            val vehicle = Vehicle(
                type = vehicleType,
                make = state.makeTextFieldState.text.toString().trim(),
                model = state.modelTextFieldState.text.toString().trim(),
                color = state.colorTextFieldState.text.toString().trim(),
                plateNumber = state.plateTextFieldState.text.toString().trim().uppercase(),
            )
            riderService.saveVehicle(vehicle)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.emit(VehicleEvent.Saved)
                }
                .onFailure { error ->
                    val errorText = when (error) {
                        DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_plate_taken)
                        DataError.Remote.BAD_REQUEST -> UiText.Resource(Res.string.error_vehicle_invalid)
                        else -> error.toUiText()
                    }
                    _state.update { it.copy(isSaving = false, errorText = errorText) }
                }
        }
    }

    private fun TextFieldState.textFlow() = snapshotFlow { text.toString() }

    private fun TextFieldState.setText(value: String) {
        edit { replace(0, length, value) }
    }
}
