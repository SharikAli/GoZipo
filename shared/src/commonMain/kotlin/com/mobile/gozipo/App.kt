package com.mobile.gozipo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mobile.auth.presentation.navigation.AuthGraphRoutes
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.presentation.util.ObserveAsEvents
import com.mobile.gozipo.navigation.DeepLinkListener
import com.mobile.gozipo.navigation.HomeRoute
import com.mobile.gozipo.navigation.NavigationRoot
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onAuthenticationChecked: () -> Unit = {},
    onDeepLinkListenerSetup: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isCheckingAuth) {
        if(!state.isCheckingAuth) {
            onAuthenticationChecked()
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when(event) {
            is MainEvent.OnSessionExpired -> {
                navController.navigate(AuthGraphRoutes.Graph) {
                    // Clear everything, including logged-in screens
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            }
        }
    }

    GoZipoTheme(darkTheme = isDarkTheme) {
        if(!state.isCheckingAuth) {
            NavigationRoot(
                navController = navController,
                startDestination = if(state.isLoggedIn) {
                    HomeRoute
                } else {
                    AuthGraphRoutes.Graph
                },
                onLogoutClick = viewModel::logout
            )
            DeepLinkListener(navController, onDeepLinkListenerSetup)
        }
    }
}