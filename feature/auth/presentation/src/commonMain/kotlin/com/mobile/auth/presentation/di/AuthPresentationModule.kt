package com.mobile.auth.presentation.di

import com.mobile.auth.presentation.email_verification.EmailVerificationViewModel
import com.mobile.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.mobile.auth.presentation.login.LoginViewModel
import com.mobile.auth.presentation.passkey_signup.PasskeySignupViewModel
import com.mobile.auth.presentation.register.RegisterViewModel
import com.mobile.auth.presentation.register_success.RegisterSuccessViewModel
import com.mobile.auth.presentation.reset_password.ResetPasswordViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::RegisterViewModel)
    viewModelOf(::RegisterSuccessViewModel)
    viewModelOf(::EmailVerificationViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ResetPasswordViewModel)
    viewModelOf(::PasskeySignupViewModel)
}