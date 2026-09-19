package com.triggerapp.feature.home.di

import com.triggerapp.feature.home.presentation.conversations.ConversationsViewModel
import com.triggerapp.feature.home.presentation.shell.HomeViewModel
import com.triggerapp.feature.home.presentation.users.UsersViewModel
import com.triggerapp.feature.profile.presentation.EditProfileViewModel
import com.triggerapp.feature.profile.presentation.ProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for home feature ViewModels (shell, conversations, users) plus profile tab.
 * @author udit
 */
val homeFeatureModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::UsersViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditProfileViewModel)
}
