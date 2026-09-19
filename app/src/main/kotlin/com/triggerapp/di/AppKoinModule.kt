package com.triggerapp.di

import com.triggerapp.splash.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for app-level [androidx.lifecycle.ViewModel] instances (e.g. splash coordinated with [com.triggerapp.MainActivity]).
 * @author udit
 */
val appModule = module {
    viewModelOf(::SplashViewModel)
}
