package com.triggerapp.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.coroutines.stateInWhileSubscribed
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.model.FollowState
import com.triggerapp.domain.model.User
import com.triggerapp.domain.usecase.social.ObserveFollowerIdsUseCase
import com.triggerapp.domain.usecase.social.ObserveFollowStateUseCase
import com.triggerapp.domain.usecase.social.ObserveFollowingIdsUseCase
import com.triggerapp.domain.usecase.social.SetFollowingUseCase
import com.triggerapp.domain.usecase.user.FetchUsersByIdsUseCase
import com.triggerapp.domain.usecase.user.ObserveUserProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [androidx.lifecycle.ViewModel] for peer profile: [state] + [onEvent].
 *
 * Streams the peer's live profile together with the real follow relationship
 * (`Followers`/`Following` nodes) — replacing the old hash-fake counters and
 * local-only follow state. Follower/following id lists are resolved to full
 * [User] profiles for the connection sheets.
 *
 * @param savedStateHandle Contains [TriggerStrings.Nav.ARG_PROFILE_USER_ID].
 * @param observeUserProfile Domain port for `Users/{id}`.
 * @param observeFollowState Live follow snapshot for the peer.
 * @param observeFollowerIds Live follower-id list of the peer.
 * @param observeFollowingIds Live following-id list of the peer.
 * @param fetchUsersByIds Resolves uids to profiles for the sheets.
 * @param setFollowing Writes follows/unfollows.
 * @author udit
 */
class PeerProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val observeFollowState: ObserveFollowStateUseCase,
    observeFollowerIds: ObserveFollowerIdsUseCase,
    observeFollowingIds: ObserveFollowingIdsUseCase,
    private val fetchUsersByIds: FetchUsersByIdsUseCase,
    private val setFollowing: SetFollowingUseCase,
) : ViewModel() {
    val profileUserId: String = checkNotNull(
        savedStateHandle.get<String>(TriggerStrings.Nav.ARG_PROFILE_USER_ID),
    )

    private val _retry = MutableStateFlow(0)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _followBusy = MutableStateFlow(false)
    private val _followError = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userFlow = _retry.flatMapLatest {
        observeUserProfile(profileUserId)
            .catch { e ->
                _loadError.value = e.userFacingMessage(
                    offlineFallback = TriggerStrings.Errors.LOAD_PEOPLE_FAILED,
                    genericFallback = TriggerStrings.Errors.LOAD_PEOPLE_FAILED,
                )
                emit(null)
            }
    }

    /** Live follow snapshot for this peer. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val followFlow = _retry.flatMapLatest {
        observeFollowState(profileUserId)
            .catch { e ->
                _followError.value = e.userFacingMessage(
                    offlineFallback = TriggerStrings.Errors.FOLLOW_ACTION_FAILED,
                    genericFallback = TriggerStrings.Errors.FOLLOW_ACTION_FAILED,
                )
                emit(FollowState())
            }
    }

    /** Resolved follower profiles for the Followers sheet. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val followersUsersFlow = _retry.flatMapLatest {
        observeFollowerIds(profileUserId).flatMapLatest { ids -> resolveUsers(ids) }
    }

    /** Resolved following profiles for the Following sheet. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val followingUsersFlow = _retry.flatMapLatest {
        observeFollowingIds(profileUserId).flatMapLatest { ids -> resolveUsers(ids) }
    }

    private fun resolveUsers(ids: List<String>) = flow {
        val users = if (ids.isEmpty()) emptyList() else {
            fetchUsersByIds(ids).getOrElse { emptyList() }
        }
        emit(users)
    }

    /**
     * Single UI snapshot; successful User clears surfaced [PeerProfileUiState.loadError].
     * @author udit
     */
    val state: StateFlow<PeerProfileUiState> = combine(
        userFlow.stateInWhileSubscribed(viewModelScope, null),
        followFlow.stateInWhileSubscribed(viewModelScope, FollowState()),
        followersUsersFlow.stateInWhileSubscribed(viewModelScope, emptyList()),
        followingUsersFlow.stateInWhileSubscribed(viewModelScope, emptyList()),
        combine(_followBusy, _followError, _loadError) { busy, fErr, lErr -> Triple(busy, fErr, lErr) },
    ) { user, follow, followers, following, flags ->
        PeerProfileUiState(
            user = user,
            loadError = if (user != null) null else flags.third,
            follow = follow,
            followersUsers = followers,
            followingUsers = following,
            followBusy = flags.first,
            followError = flags.second,
        )
    }.stateInWhileSubscribed(
        scope = viewModelScope,
        initialValue = PeerProfileUiState(),
    )

    /**
     *
     * @param event User intent.
     * @author udit
     */
    fun onEvent(event: PeerProfileUiEvent) {
        when (event) {
            PeerProfileUiEvent.Retry -> {
                _loadError.value = null
                _retry.update { it + 1 }
            }
            PeerProfileUiEvent.ToggleFollow -> toggleFollow()
            PeerProfileUiEvent.ConsumeFollowError -> _followError.value = null
        }
    }

    private fun toggleFollow() {
        val current = state.value
        if (current.followBusy || current.user == null) return
        val target = !current.follow.isFollowing
        _followBusy.value = true
        viewModelScope.launch {
            setFollowing(profileUserId, target).fold(
                onSuccess = {
                    _followBusy.value = false
                    _followError.value = null
                },
                onFailure = { e ->
                    _followBusy.value = false
                    _followError.value = e.userFacingMessage(
                        offlineFallback = TriggerStrings.Errors.FOLLOW_ACTION_FAILED,
                        genericFallback = TriggerStrings.Errors.FOLLOW_ACTION_FAILED,
                    )
                },
            )
        }
    }
}
