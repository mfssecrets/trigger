package com.triggerapp.data.di

import com.triggerapp.data.connectivity.NetworkConnectivityImpl
import com.triggerapp.data.repository.firebase.AuthRepositoryImpl
import com.triggerapp.data.repository.firebase.ChatRepositoryImpl
import com.triggerapp.data.repository.firebase.FcmTokenRepositoryImpl
import com.triggerapp.data.repository.firebase.OtpRepositoryImpl
import com.triggerapp.data.repository.firebase.SocialRepositoryImpl
import com.triggerapp.data.repository.firebase.UserRepositoryImpl
import com.triggerapp.data.local.OpenChatStore
import com.triggerapp.data.push.FirebasePushTokenReader
import com.triggerapp.domain.connectivity.NetworkConnectivity
import com.triggerapp.domain.repository.AuthRepository
import com.triggerapp.domain.repository.ChatRepository
import com.triggerapp.domain.repository.FcmTokenRepository
import com.triggerapp.domain.repository.OpenChatTracker
import com.triggerapp.domain.repository.OtpRepository
import com.triggerapp.domain.repository.UserRepository
import com.triggerapp.domain.repository.SocialRepository
import com.triggerapp.domain.usecase.push.DevicePushTokenReader
import com.triggerapp.domain.usecase.chat.FetchChatListPageUseCase
import com.triggerapp.domain.usecase.user.FetchUsersByIdsUseCase
import com.triggerapp.domain.usecase.user.FetchUsersDirectoryPageUseCase
import com.triggerapp.domain.usecase.chat.MarkPeerMessagesSeenUseCase
import com.triggerapp.domain.usecase.chat.ObserveChatPartnerIdsUseCase
import com.triggerapp.domain.usecase.chat.ObserveConversationMessagesUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveUserProfileUseCase
import com.triggerapp.domain.usecase.push.RegisterPushTokenUseCase
import com.triggerapp.domain.usecase.chat.SendConversationMessageUseCase
import com.triggerapp.domain.usecase.chat.SetActiveChatPeerUseCase
import com.triggerapp.domain.usecase.presence.SetUserPresenceUseCase
import com.triggerapp.domain.usecase.social.AddPostCommentUseCase
import com.triggerapp.domain.usecase.social.CreatePostUseCase
import com.triggerapp.domain.usecase.social.MarkAllNotificationsReadUseCase
import com.triggerapp.domain.usecase.social.ObserveFeedUseCase
import com.triggerapp.domain.usecase.social.ObserveFollowStateUseCase
import com.triggerapp.domain.usecase.social.ObserveFollowerIdsUseCase
import com.triggerapp.domain.usecase.social.ObserveFollowingIdsUseCase
import com.triggerapp.domain.usecase.social.ObserveIsFollowingUseCase
import com.triggerapp.domain.usecase.social.ObserveLikedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ObserveNotificationsUseCase
import com.triggerapp.domain.usecase.social.ObservePeerFollowsMeUseCase
import com.triggerapp.domain.usecase.social.ObservePostCommentsUseCase
import com.triggerapp.domain.usecase.social.ObserveSavedPostIdsUseCase
import com.triggerapp.domain.usecase.social.ReportPostUseCase
import com.triggerapp.domain.usecase.social.SetFollowingUseCase
import com.triggerapp.domain.usecase.social.SetPostLikedUseCase
import com.triggerapp.domain.usecase.social.SetPostSavedUseCase
import com.triggerapp.domain.usecase.auth.SendPasswordResetUseCase
import com.triggerapp.domain.usecase.auth.SendOtpUseCase
import com.triggerapp.domain.usecase.auth.SignInUseCase
import com.triggerapp.domain.usecase.auth.SignOutUseCase
import com.triggerapp.domain.usecase.auth.SignUpUseCase
import com.triggerapp.domain.usecase.auth.SignUpWithOtpUseCase
import com.triggerapp.domain.usecase.auth.StreamAuthSessionUseCase
import com.triggerapp.domain.usecase.auth.CheckUsernameAvailabilityUseCase
import com.triggerapp.domain.usecase.auth.ResetPasswordWithOtpUseCase
import com.triggerapp.domain.usecase.auth.VerifyOtpUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.VerifyFaceUseCase
import com.triggerapp.domain.usecase.user.UpdateBioUseCase
import com.triggerapp.domain.usecase.user.UpdateProfileFieldsUseCase
import com.triggerapp.domain.usecase.user.UpdateUsernameUseCase
import com.triggerapp.domain.usecase.user.UploadProfileImageUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module: Firebase singletons, [com.triggerapp.data.repository.firebase] implementations,
 * connectivity, [OpenChatTracker] (backed by [com.triggerapp.data.local.OpenChatStore]), use cases, and push token reader.
 * @author udit
 */
val dataModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseDatabase.getInstance() }
    single { com.google.firebase.functions.FirebaseFunctions.getInstance() }
    single<NetworkConnectivity> { NetworkConnectivityImpl(androidContext()) }
    single<OpenChatTracker> { OpenChatStore(androidContext()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
    single<SocialRepository> { SocialRepositoryImpl(get(), get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    single<FcmTokenRepository> { FcmTokenRepositoryImpl(get()) }
    single<OtpRepository> { OtpRepositoryImpl(get(), get(), get()) }

    single<DevicePushTokenReader> { FirebasePushTokenReader() }

    factoryOf(::StreamAuthSessionUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SendPasswordResetUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::SendOtpUseCase)
    factoryOf(::VerifyOtpUseCase)
    factoryOf(::SignUpWithOtpUseCase)
    factoryOf(::ResetPasswordWithOtpUseCase)
    factoryOf(::CheckUsernameAvailabilityUseCase)
    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::UpdateUsernameUseCase)
    factoryOf(::UpdateBioUseCase)
    factoryOf(::UpdateProfileFieldsUseCase)
    factoryOf(::VerifyFaceUseCase)
    factoryOf(::UploadProfileImageUseCase)
    factoryOf(::ObserveNetworkOnlineUseCase)
    factoryOf(::RegisterPushTokenUseCase)
    factoryOf(::SetUserPresenceUseCase)
    factoryOf(::FetchUsersDirectoryPageUseCase)
    factoryOf(::ObserveChatPartnerIdsUseCase)
    factoryOf(::FetchChatListPageUseCase)
    factoryOf(::FetchUsersByIdsUseCase)
    factoryOf(::ObserveConversationMessagesUseCase)
    factoryOf(::ObserveUserProfileUseCase)
    factoryOf(::SendConversationMessageUseCase)
    factoryOf(::MarkPeerMessagesSeenUseCase)
    factoryOf(::SetActiveChatPeerUseCase)
    factoryOf(::ObserveFeedUseCase)
    factoryOf(::CreatePostUseCase)
    factoryOf(::SetPostLikedUseCase)
    factoryOf(::ObserveLikedPostIdsUseCase)
    factoryOf(::ObserveSavedPostIdsUseCase)
    factoryOf(::SetPostSavedUseCase)
    factoryOf(::ReportPostUseCase)
    factoryOf(::ObservePostCommentsUseCase)
    factoryOf(::AddPostCommentUseCase)
    factoryOf(::ObserveIsFollowingUseCase)
    factoryOf(::ObservePeerFollowsMeUseCase)
    factoryOf(::ObserveFollowerIdsUseCase)
    factoryOf(::ObserveFollowingIdsUseCase)
    factoryOf(::SetFollowingUseCase)
    factoryOf(::ObserveFollowStateUseCase)
    factoryOf(::ObserveNotificationsUseCase)
    factoryOf(::MarkAllNotificationsReadUseCase)
}
