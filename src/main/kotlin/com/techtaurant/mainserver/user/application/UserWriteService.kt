package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.user.dto.UpdateUserRequest
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserWriteService(
    private val userRepository: UserRepository,
    private val attachmentService: AttachmentService,
    private val userResponseAssembler: UserResponseAssembler,
) {
    companion object {
        private const val USER_NAME_UNIQUE_CONSTRAINT = "uk_users_name"
    }

    @Transactional
    fun updateMe(
        userId: UUID,
        request: UpdateUserRequest,
    ): UserResponse {
        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }

        request.name?.let { requestedName ->
            val normalizedName = requestedName.trim()
            if (normalizedName.isEmpty()) {
                throw ApiException(DefaultStatus.BAD_REQUEST, "이름은 공백일 수 없습니다")
            }
            user.name = normalizedName

            try {
                userRepository.flush()
            } catch (exception: DataIntegrityViolationException) {
                if (isUserNameUniqueConstraintViolation(exception)) {
                    throw ApiException(UserStatus.USER_NAME_ALREADY_EXISTS)
                }
                throw exception
            }
        }

        if (request.hasServiceProfileImageAttachmentId()) {
            val attachmentId = request.parseServiceProfileImageAttachmentId()
            user.serviceProfileImageAttachmentId = attachmentId

            if (attachmentId != null) {
                attachmentService.confirmAttachmentsByIds(
                    referenceId = userId,
                    referenceType = AttachmentReferenceType.USER,
                    attachmentIds = listOf(attachmentId),
                )
            }

            attachmentService.deleteOrphanedAttachmentsByIds(
                referenceId = userId,
                referenceType = AttachmentReferenceType.USER,
                keepAttachmentIds = listOfNotNull(attachmentId),
            )
        }

        return userResponseAssembler.assemble(user)
    }

    private fun isUserNameUniqueConstraintViolation(exception: DataIntegrityViolationException): Boolean {
        return generateSequence<Throwable>(exception) { current -> current.cause }
            .mapNotNull { throwable -> throwable.message }
            .any { message -> message.contains(USER_NAME_UNIQUE_CONSTRAINT, ignoreCase = true) }
    }
}
