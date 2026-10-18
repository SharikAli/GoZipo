package com.mobile.gozipo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobile.apps.common.navigation.emailLinkBaseUrl
import com.mobile.auth.presentation.navigation.AuthGraphRoutes
import com.mobile.auth.presentation.navigation.authGraph
import com.mobile.booking.presentation.booking.BookingRoot
import com.mobile.core.domain.auth.UserRole
import com.mobile.payment.presentation.methods.PaymentMethodsRoot
import kotlinx.serialization.Serializable

/** Logged-in home: the map where the customer books and follows a ride. */
@Serializable
data object HomeRoute

/** Saved cards: add (SetupIntent) and remove. */
@Serializable
data object PaymentMethodsRoute

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
            emailLinkBaseUrl = emailLinkBaseUrl(UserRole.CUSTOMER),
            onLoginSuccess = {
                navController.navigate(HomeRoute) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            }
        )
        composable<HomeRoute> {
            BookingRoot(
                onLogoutClick = onLogoutClick,
                onPaymentMethodsClick = { navController.navigate(PaymentMethodsRoute) }
            )
        }
        composable<PaymentMethodsRoute> {
            PaymentMethodsRoot(onBackClick = { navController.navigateUp() })
        }
    }
}
