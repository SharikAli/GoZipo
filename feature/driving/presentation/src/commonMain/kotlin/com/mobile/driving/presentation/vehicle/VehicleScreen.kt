package com.mobile.driving.presentation.vehicle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile.core.designsystem.components.buttons.GoZipoButton
import com.mobile.core.designsystem.components.layouts.GoZipoAuthFormLayout
import com.mobile.core.designsystem.components.layouts.GoZipoSnackbarScaffold
import com.mobile.core.designsystem.components.ride.DefaultVehicleImage
import com.mobile.core.designsystem.components.ride.SectionLabel
import com.mobile.core.designsystem.components.ride.VehicleOptionItem
import com.mobile.core.designsystem.components.textfields.GoZipoTextField
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.ride.domain.VehicleType
import com.mobile.ride.presentation.util.toUi
import gozipo.feature.driving.presentation.generated.resources.Res
import gozipo.feature.driving.presentation.generated.resources.back
import gozipo.feature.driving.presentation.generated.resources.save
import gozipo.feature.driving.presentation.generated.resources.vehicle_color
import gozipo.feature.driving.presentation.generated.resources.vehicle_color_hint
import gozipo.feature.driving.presentation.generated.resources.vehicle_make
import gozipo.feature.driving.presentation.generated.resources.vehicle_make_hint
import gozipo.feature.driving.presentation.generated.resources.vehicle_model
import gozipo.feature.driving.presentation.generated.resources.vehicle_model_hint
import gozipo.feature.driving.presentation.generated.resources.vehicle_plate
import gozipo.feature.driving.presentation.generated.resources.vehicle_plate_hint
import gozipo.feature.driving.presentation.generated.resources.vehicle_subtitle
import gozipo.feature.driving.presentation.generated.resources.vehicle_title_add
import gozipo.feature.driving.presentation.generated.resources.vehicle_title_edit
import gozipo.feature.driving.presentation.generated.resources.vehicle_type
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VehicleRoot(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VehicleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            VehicleEvent.Saved -> onSaved()
        }
    }

    VehicleScreen(
        state = state,
        onAction = { action ->
            when (action) {
                VehicleAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
fun VehicleScreen(
    state: VehicleState,
    onAction: (VehicleAction) -> Unit,
) {
    val spacing = GoZipoTheme.spacing

    GoZipoSnackbarScaffold {
        GoZipoAuthFormLayout(
            title = stringResource(if (state.isEditing) Res.string.vehicle_title_edit else Res.string.vehicle_title_add),
            subtitle = stringResource(Res.string.vehicle_subtitle),
            errorText = state.errorText?.asString(),
            onBackClick = { onAction(VehicleAction.OnBackClick) },
            backContentDescription = stringResource(Res.string.back),
            logo = {},
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.extended.textPrimary,
                    trackColor = MaterialTheme.colorScheme.extended.surfaceHigher,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(spacing.md))
            }

            SectionLabel(text = stringResource(Res.string.vehicle_type))
            Spacer(modifier = Modifier.height(spacing.xs))
            VehicleType.entries.forEach { type ->
                val ui = type.toUi()
                VehicleOptionItem(
                    title = stringResource(ui.title),
                    capacityText = ui.seats.toString(),
                    etaText = null,
                    description = stringResource(ui.description),
                    priceText = null,
                    selected = state.vehicleType == type,
                    onClick = { onAction(VehicleAction.OnVehicleTypeClick(type)) },
                    image = { DefaultVehicleImage(icon = ui.icon) }
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))
            VehicleField(
                state = state.makeTextFieldState,
                title = stringResource(Res.string.vehicle_make),
                placeholder = stringResource(Res.string.vehicle_make_hint),
                maxLength = VehicleState.MAX_MAKE_LENGTH,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            VehicleField(
                state = state.modelTextFieldState,
                title = stringResource(Res.string.vehicle_model),
                placeholder = stringResource(Res.string.vehicle_model_hint),
                maxLength = VehicleState.MAX_MODEL_LENGTH,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            VehicleField(
                state = state.colorTextFieldState,
                title = stringResource(Res.string.vehicle_color),
                placeholder = stringResource(Res.string.vehicle_color_hint),
                maxLength = VehicleState.MAX_COLOR_LENGTH,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            VehicleField(
                state = state.plateTextFieldState,
                title = stringResource(Res.string.vehicle_plate),
                placeholder = stringResource(Res.string.vehicle_plate_hint),
                maxLength = VehicleState.MAX_PLATE_LENGTH,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
                onDone = { onAction(VehicleAction.OnSaveClick) }
            )

            Spacer(modifier = Modifier.height(spacing.xl))
            GoZipoButton(
                text = stringResource(Res.string.save),
                onClick = { onAction(VehicleAction.OnSaveClick) },
                enabled = state.canSave,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VehicleField(
    state: TextFieldState,
    title: String,
    placeholder: String,
    maxLength: Int,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    GoZipoTextField(
        state = state,
        title = title,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        onKeyboardAction = onDone?.let { done -> { done() } },
        inputTransformation = InputTransformation.maxLength(maxLength),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview
@Composable
private fun VehicleAddPreview() {
    GoZipoTheme(darkTheme = true) {
        VehicleScreen(
            state = VehicleState(isLoading = false),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun VehicleEditPreview() {
    GoZipoTheme(darkTheme = true) {
        VehicleScreen(
            state = VehicleState(
                vehicleType = VehicleType.MINI,
                makeTextFieldState = TextFieldState("Suzuki"),
                modelTextFieldState = TextFieldState("Alto"),
                colorTextFieldState = TextFieldState("White"),
                plateTextFieldState = TextFieldState("LEA-1234"),
                isEditing = true,
                isLoading = false,
                areFieldsFilled = true,
            ),
            onAction = {}
        )
    }
}
