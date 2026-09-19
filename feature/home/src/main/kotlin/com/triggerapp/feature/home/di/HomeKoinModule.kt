package com.triggerapp.feature.home.di

import com.triggerapp.domain.usecase.social.AddPostCommentUseCase
import com.triggerapp.domain.usecase.social.CreatePostUseCase
import com.triggerapp.domain.usecase.social.ObserveFeedUseCase
import com.triggerapp.domain.usecase.social.ObserveLikedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ObservePostCommentsUseCase
import com.triggerapp.domain.usecase.social.ObserveSavedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ReportPostUseCase
import com.triggerapp.domain.usecase.social.SetPostLikedUseCase
import com.triggerapp.domain.usecase.social.SetPostSavedUseCase
import com.triggerapp.feature.home.presentation.conversations.ConversationsViewModel
import com.triggerapp.feature.home.presentation.feed.FeedViewModel
import com.triggerapp.feature.home.presentation.notifications.NotificationsViewModel
import com.triggerapp.feature.home.presentation.shell.HomeViewModel
import com.triggerapp.feature.home.presentation.users.UsersViewModel
import com.triggerapp.feature.profile.presentation.EditProfileViewModel
import com.triggerapp.feature.profile.presentation.ProfileViewModel
import com.triggerapp.feature.profile.presentation.VerificationViewModel
import com.triggerapp.feature.profile.verification.GenderClassifier
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for home feature ViewModels (shell, conversations, feed, notifications,
 * users) plus the profile tab.
 * @author udit
 */
val homeFeatureModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::UsersViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::VerificationViewModel)
    viewModelOf(::NotificationsViewModel)

    // FeedViewModel needs the app context to decode picked images into JPEG bytes.
    viewModel {
        FeedViewModel(
            context = androidContext(),
            observeFeed = get(),
            observeLikedPostIds = get(),
            observeSavedPostIds = get(),
            createPost = get(),
            setPostLiked = get(),
            setPostSaved = get(),
            reportPost = get(),
            observePostComments = get(),
            addPostComment = get(),
        )
    }

    // One classifier per verification session; closed with the owning ViewModel.
    factory { GenderClassifier(androidContext()) }
}
