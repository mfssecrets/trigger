package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.repository.UserRepository

/**
 * Application use case: write simple profile fields (display name, gender, date of birth)
 * in one multi-path update on the signed-in user node.
 *
 * @author udit
 */
class UpdateProfileFieldsUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param fields Child-name to value map (e.g. `displayName`, `gender`, `dob`).
     * @return [Result] success when the update completes.
     * @author udit
     */
    suspend operator fun invoke(fields: Map<String, String>): Result<Unit> =
        userRepository.updateProfileFields(fields)
}
