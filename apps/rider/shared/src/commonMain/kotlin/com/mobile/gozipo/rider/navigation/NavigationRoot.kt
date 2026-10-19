package com.mobile.gozipo.rider.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mobile.apps.common.navigation.emailLinkBaseUrl
import com.mobile.auth.presentation.navigation.AuthGraphRoutes
import com.mobile.auth.presentation.navigation.authGraph
import com.mobile.core.domain.auth.UserRole
import com.mobile.driving.presentation.driving.DrivingRoot
import com.mobile.driving.presentation.history.RideHistoryRoot
import com.mobile.driving.presentation.navigation.NavigationTarget
import com.mobile.driving.presentation.navigation.TurnByTurnRoot
import com.mobile.driving.presentation.routes.EarningsRoute
import com.mobile.driving.presentation.routes.HomeRoute
import com.mobile.driving.presentation.routes.TurnByTurnRoute
import com.mobile.driving.presentation.routes.VehicleRoute
import com.mobile.driving.presentation.vehicle.VehicleRoot

@Composable
fun NavigationRoot(
    navController: NavHostController,
    startDestination: Any,
    onLogoutClick: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authGraph(
            navController = navController,
            emailLinkBaseUrl = emailLinkBaseUrl(UserRole.RIDER),
            onLoginSuccess = {
                navController.navigate(HomeRoute) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            }
        )
        composable<HomeRoute> {
            DrivingRoot(
                onLogoutClick = onLogoutClick,
                onVehicleClick = {
                    navController.navigate(VehicleRoute) { launchSingleTop = true }
                },
                onEarningsClick = {
                    navController.navigate(EarningsRoute) { launchSingleTop = true }
                },
                onNavigateClick = { target ->
                    navController.navigate(target.toRoute()) { launchSingleTop = true }
                }
            )
        }
        composable<VehicleRoute> {
            VehicleRoot(
                onBackClick = { navController.navigateUp() },
                onSaved = { navController.navigateUp() }
            )
        }
        composable<EarningsRoute> {
            RideHistoryRoot(onBackClick = { navController.navigateUp() })
        }
        composable<TurnByTurnRoute> { entry ->
            TurnByTurnRoot(
                target = entry.toRoute<TurnByTurnRoute>().toTarget(),
                onFinished = { navController.navigateUp() }
            )
        }
    }
}

private fun NavigationTarget.toRoute() = TurnByTurnRoute(
    rideId = rideId,
    latitude = destination.latitude,
    longitude = destination.longitude,
    destinationName = destinationName,
    originLatitude = origin?.latitude,
    originLongitude = origin?.longitude
)