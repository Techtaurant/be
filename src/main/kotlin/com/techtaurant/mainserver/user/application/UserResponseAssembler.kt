package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.entity.User
import org.springframework.stereotype.Component

@Component
class UserResponseAssembler(
    private val attachmentService: AttachmentService,
) {
    fun assemble(user: User): UserResponse {
        return assemble(listOf(user)).first()
    }

    fun assemble(users: List<User>): List<UserResponse> {
        if (users.isEmpty()) {
            return emptyList()
        }

        val userIds = users.mapNotNull { it.id }
        val attachmentsByUserId =
            if (userIds.isNotEmpty()) {
                attachmentService.getConfirmedAttachmentsByReferenceIds(userIds, AttachmentReferenceType.USER)
            } else {
                emptyMap()
            }
        val presignedUrlByAttachmentId =
            attachmentService.generatePresignedDownloadUrlMapByAttachments(
                attachmentsByUserId.values.flatten(),
            )

        return users.map { user ->
            val profileImageUrl =
                user.serviceProfileImageAttachmentId
                    ?.let { presignedUrlByAttachmentId[it] }
                    ?: user.profileImageUrl
            UserResponse.from(user, profileImageUrl)
        }
    }
}
