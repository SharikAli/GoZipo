package com.mobile.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.mobile.auth.presentation.email_verification.EmailVerificationRoot
import com.mobile.auth.presentation.forgot_password.ForgotPasswordRoot
import com.mobile.auth.presentation.login.LoginRoot
import com.mobile.auth.presentation.passkey_signup.PasskeySignupRoot
import com.mobile.auth.presentation.register.RegisterRoot
import com.mobile.auth.presentation.register_success.RegisterSuccessRoot
import com.mobile.auth.presentation.reset_password.ResetPasswordRoot

fun NavGraphBuilder.authGraph(
    navController: NavController,
    onLoginSuccess: () -> Unit,
) {
    navigation<AuthGraphRoutes.Graph>(
        startDestination = AuthGraphRoutes.Login
    ) {
        composable<AuthGraphRoutes.Login> {
            LoginRoot(
                onLoginSuccess = onLoginSuccess,
                onForgotPasswordClick = {
                    navController.navigate(AuthGraphRoutes.ForgotPassword)
                },
                onCreateAccountClick = {
                    navController.navigate(AuthGraphRoutes.Register) {
                        restoreState = true
                        launchSingleTop = true
                    }
                },
                navigateToEmailVerificationScreen = {
                    navController.navigate(AuthGraphRoutes.RegisterSuccess(it, true))
                },
                navigateToPasskeySignup = { email ->
                    navController.navigate(AuthGraphRoutes.PasskeySignup(email))
                }
            )
        }
        composable<AuthGraphRoutes.Register> {
            RegisterRoot(
                onRegisterSuccess = {
                    navController.navigate(AuthGraphRoutes.RegisterSuccess(it))
                },
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo(AuthGraphRoutes.Register) {
                            inclusive = true
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onPasskeySignupClick = {
                    navController.navigate(AuthGraphRoutes.PasskeySignup())
                }
            )
        }
        composable<AuthGraphRoutes.PasskeySignup> {
            PasskeySignupRoot(
                onSignupSuccess = onLoginSuccess,
                onBackClick = { navController.navigateUp() }
            )
        }
        composable<AuthGraphRoutes.RegisterSuccess> {
            RegisterSuccessRoot(
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.RegisterSuccess> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<AuthGraphRoutes.EmailVerification>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://gozipo.coding.com/api/auth/verify?token={token}"
                },
                navDeepLink {
                    uriPattern = "gozipo://gozipo.coding.com/api/auth/verify?token={token}"
                },
                navDeepLink {
                    uriPattern = "http://localhost:8080/api/auth/verify?token={token}"
                },
            )
        ) {
            EmailVerificationRoot(
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.EmailVerification> {
                            inclusive = true
                        }
                    }
                },
                onCloseClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.EmailVerification> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<AuthGraphRoutes.ForgotPassword> {
            ForgotPasswordRoot(
                onBackClick = { navController.navigateUp() }
            )
        }
        composable<AuthGraphRoutes.ResetPassword>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://gozipo.coding.com/api/auth/reset-password?token={token}"
                },
                navDeepLink {
                    uriPattern = "gozipo://gozipo.coding.com/api/auth/reset-password?token={token}"
                },
                navDeepLink {
                    uriPattern = "http://localhost:8080/api/auth/reset-password?token={token}"
                },
            )
        ) {
            ResetPasswordRoot(
                onLoginClick = {
                    navController.navigate(AuthGraphRoutes.Login) {
                        popUpTo<AuthGraphRoutes.ResetPassword> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}