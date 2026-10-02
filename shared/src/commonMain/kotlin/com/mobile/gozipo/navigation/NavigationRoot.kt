package com.mobile.gozipo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobile.auth.presentation.navigation.AuthGraphRoutes
import com.mobile.auth.presentation.navigation.authGraph
import com.mobile.gozipo.home.HomePlaceholderScreen
import kotlinx.serialization.Serializable

/** Temporary logged-in destination until the customer map flow exists. */
@Serializable
data object HomeRoute

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
            onLoginSuccess = {
                navController.navigate(HomeRoute) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            }
        )
        composable<HomeRoute> {
            HomePlaceholderScreen(onLogoutClick = onLogoutClick)
        }
    }
}
