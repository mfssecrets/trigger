package com.triggerapp.domain.usecase.chat

import com.triggerapp.domain.repository.OpenChatTracker

/**
 * Notifies the app which peer chat is foreground (for notification routing).
 * @author udit
 */
class SetActiveChatPeerUseCase(
    private val openChatTracker: OpenChatTracker,
) {
    /**
     *
     * @param peerId Active peer uid, or `null` when leaving the thread.
     * @author udit
     */
    operator fun invoke(peerId: String?) {
        openChatTracker.setActivePeer(peerId)
    }
}
