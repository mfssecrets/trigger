package com.triggerapp.domain.usecase.chat

import com.triggerapp.domain.model.PagedChatListResult
import com.triggerapp.domain.repository.ChatRepository

/**
 * One page of the conversation list ordered by activity timestamp.
 * @author udit
 */
class FetchChatListPageUseCase(
    private val chatRepository: ChatRepository,
) {
    /**
     *
     * @param currentUserId Owner of `ChatList/{uid}`.
     * @param pageSize Page size (repository may use +1 for overflow).
     * @param endBeforeTimestamp Cursor for older pages, or `null` for newest.
     * @param endBeforeKey Partner key paired with [endBeforeTimestamp].
     * @author udit
     */
    suspend operator fun invoke(
        currentUserId: String,
        pageSize: Int,
        endBeforeTimestamp: String?,
        endBeforeKey: String?,
    ): Result<PagedChatListResult> = chatRepository.fetchChatListPage(
        currentUserId = currentUserId,
        pageSize = pageSize,
        endBeforeTimestamp = endBeforeTimestamp,
        endBeforeKey = endBeforeKey,
    )
}
