package com.mobile.driving.presentation.vehicle

import androidx.compose.foundation.text.input.TextFieldState
import com.mobile.core.presentation.util.UiText
import com.mobile.ride.domain.VehicleType

data class VehicleState(
    val vehicleType: VehicleType? = null,
    val makeTextFieldState: TextFieldState = TextFieldState(),
    val modelTextFieldState: TextFieldState = TextFieldState(),
    val colorTextFieldState: TextFieldState = TextFieldState(),
    val plateTextFieldState: TextFieldState = TextFieldState(),
    /** The rider already has a vehicle; this screen edits it. */
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val areFieldsFilled: Boolean = false,
    val errorText: UiText? = null,
) {
    val canSave: Boolean
        get() = vehicleType != null && areFieldsFilled && !isLoading && !isSaving

    companion object {
        // The backend's limits
        const val MAX_MAKE_LENGTH = 40
        const val MAX_MODEL_LENGTH = 40
        const val MAX_COLOR_LENGTH = 30
        const val MAX_PLATE_LENGTH = 20
    }
}
