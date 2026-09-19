package com.triggerapp.feature.home.presentation.notifications

import com.triggerapp.domain.model.SocialNotification

/**
 * UI state for the RTDB-backed notifications tab (MVI).
 *
 * @property notifications Newest-first live rows from `Notifications/{uid}`.
 * @property isLoading True until the first snapshot arrives.
 * @property loadError Recoverable stream error message.
 * @property isMarkingRead True while "Mark all read" is running.
 * @author udit
 */
data class NotificationsUiState(
    val notifications: List<SocialNotification> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isMarkingRead: Boolean = false,
) {
    /** Number of unread rows shown in the header line. */
    val unreadCount: Int get() = notifications.count { !it.read }
}

/**
 * User intents for the notifications tab.
 * @author udit
 */
sealed interface NotificationsUiEvent {
    /** Reload after a stream error. */
    data object Retry : NotificationsUiEvent

    /** Mark every notification as read. */
    data object MarkAllRead : NotificationsUiEvent
}
