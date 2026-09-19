package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.model.User
import com.triggerapp.domain.repository.UserRepository

/**
 * Batch-loads user profiles preserving first-seen order of ids.
 * @author udit
 */
class FetchUsersByIdsUseCase(
    private val userRepository: UserRepository,
) {
    /**
     *
     * @param ids Partner uids from a chat list page.
     * @author udit
     */
    suspend operator fun invoke(ids: List<String>): Result<List<User>> =
        userRepository.fetchUsersByIds(ids)
}
