package com.mobile.driving.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobile.core.map.api.GoZipoNavigation
import com.mobile.core.presentation.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Full-screen in-app guidance to the ride's next stop. Closes on arrival (the rider then
 * confirms the step on the driving screen), when the rider ends it, or when the ride ends.
 */
@Composable
fun TurnByTurnRoot(
    target: NavigationTarget,
    onFinished: () -> Unit,
    viewModel: TurnByTurnViewModel = koinViewModel { parametersOf(target.rideId) },
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TurnByTurnEvent.RideEnded -> onFinished()
        }
    }

    GoZipoNavigation(
        destination = target.destination,
        origin = target.origin,
        destinationName = target.destinationName,
        onArrival = onFinished,
        onClose = onFinished,
        modifier = Modifier.fillMaxSize()
    )
}
