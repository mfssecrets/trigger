package com.triggerapp.feature.home.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.social.MarkAllNotificationsReadUseCase
import com.triggerapp.domain.usecase.social.ObserveNotificationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * RTDB-backed notifications [ViewModel]: streams `Notifications/{uid}` rows created by
 * real follow / like / comment / system events, and marks them read through the
 * repository. The previous hard-coded sample list is fully replaced.
 *
 * @param observeNotifications Live notification stream.
 * @param markAllNotificationsRead Marks all rows read.
 * @author udit
 */
class NotificationsViewModel(
    observeNotifications: ObserveNotificationsUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
) : ViewModel() {

    private val _extra = MutableStateFlow(NotificationsExtra())

    /** Live notifications snapshot for the tab. */
    val state: StateFlow<NotificationsUiState> = combine(
        observeNotifications().catch { e ->
            _extra.update {
                it.copy(
                    isLoading = false,
                    loadError = e.userFacingMessage(
                        offlineFallback = TriggerStrings.Errors.NOTIFICATIONS_LOAD_FAILED,
                        genericFallback = TriggerStrings.Errors.NOTIFICATIONS_LOAD_FAILED,
                    ),
                )
            }
            emit(emptyList())
        },
        _extra,
    ) { rows, extra ->
        NotificationsUiState(
            notifications = rows,
            isLoading = extra.isLoading && rows.isEmpty(),
            loadError = extra.loadError,
            isMarkingRead = extra.isMarkingRead,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(),
    )

    /**
     *
     * @param event Notifications intent.
     * @author udit
     */
    fun onEvent(event: NotificationsUiEvent) {
        when (event) {
            NotificationsUiEvent.Retry -> _extra.update { it.copy(isLoading = true, loadError = null) }
            NotificationsUiEvent.MarkAllRead -> viewModelScope.launch {
                _extra.update { it.copy(isMarkingRead = true) }
                markAllNotificationsRead().fold(
                    onSuccess = { _extra.update { it.copy(isMarkingRead = false) } },
                    onFailure = { e ->
                        _extra.update {
                            it.copy(
                                isMarkingRead = false,
                                loadError = e.userFacingMessage(
                                    offlineFallback = TriggerStrings.Errors.OFFLINE_GENERIC,
                                    genericFallback = TriggerStrings.Errors.GENERIC,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    private data class NotificationsExtra(
        val isLoading: Boolean = true,
        val isMarkingRead: Boolean = false,
        val loadError: String? = null,
    )
}
