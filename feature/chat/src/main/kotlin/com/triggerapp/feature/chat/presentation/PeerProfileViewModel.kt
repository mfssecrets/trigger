package com.triggerapp.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.coroutines.stateInWhileSubscribed
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.user.ObserveUserProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

/**
 * MVI [androidx.lifecycle.ViewModel] for peer profile: [state] + [onEvent].
 *
 *
 * @param savedStateHandle Contains [TriggerStrings.Nav.ARG_PROFILE_USER_ID].
 * @param observeUserProfile Domain port for `Users/{id}`.
 * @author udit
 */
class PeerProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val observeUserProfile: ObserveUserProfileUseCase,
) : ViewModel() {
    val profileUserId: String = checkNotNull(
        savedStateHandle.get<String>(TriggerStrings.Nav.ARG_PROFILE_USER_ID),
    )

    private val _retry = MutableStateFlow(0)
    private val _loadError = MutableStateFlow<String?>(null)

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

    /**
     * Single UI snapshot; successful User clears surfaced [PeerProfileUiState.loadError].
     * @author udit
     */
    val state: StateFlow<PeerProfileUiState> = combine(
        userFlow.stateInWhileSubscribed(viewModelScope, null),
        _loadError,
    ) { user, err ->
        PeerProfileUiState(
            user = user,
            loadError = if (user != null) null else err,
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
        }
    }
}
