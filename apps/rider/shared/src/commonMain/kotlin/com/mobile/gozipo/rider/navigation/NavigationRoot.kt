package com.mobile.gozipo.rider.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobile.apps.common.navigation.emailLinkBaseUrl
import com.mobile.auth.presentation.navigation.AuthGraphRoutes
import com.mobile.auth.presentation.navigation.authGraph
import com.mobile.apps.common.home.HomeMapRoot
import com.mobile.core.domain.auth.UserRole
import kotlinx.serialization.Serializable

/** Temporary logged-in destination until the rider ride flow exists. */
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
            HomeMapRoot(onLogoutClick = onLogoutClick)
        }
    }
}
