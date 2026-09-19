package com.triggerapp.feature.auth.di

import com.triggerapp.feature.auth.presentation.forgot.ForgotViewModel
import com.triggerapp.feature.auth.presentation.login.LoginViewModel
import com.triggerapp.feature.auth.presentation.newpassword.NewPasswordViewModel
import com.triggerapp.feature.auth.presentation.otp.OtpViewModel
import com.triggerapp.feature.auth.presentation.register.RegisterViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the auth feature.
 *
 * Binds ViewModels under [com.triggerapp.feature.auth.presentation]; Compose entry points live under
 * [com.triggerapp.feature.auth.ui].
 *
 * @author udit
 */
val authFeatureModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ForgotViewModel)
    viewModelOf(::OtpViewModel)
    viewModelOf(::NewPasswordViewModel)
}
