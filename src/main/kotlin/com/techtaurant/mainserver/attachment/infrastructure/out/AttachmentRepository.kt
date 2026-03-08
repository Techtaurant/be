package com.techtaurant.mainserver.attachment.infrastructure.out

import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AttachmentRepository : JpaRepository<Attachment, UUID> {
    fun findAllByObjectKeyInAndStatus(
        objectKeys: List<String>,
        status: AttachmentStatus,
    ): List<Attachment>

    fun findAllByReferenceIdAndReferenceType(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
    ): List<Attachment>

    fun findAllByReferenceIdInAndReferenceType(
        referenceIds: List<UUID>,
        referenceType: AttachmentReferenceType,
    ): List<Attachment>

    fun deleteAllByReferenceIdAndReferenceType(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
    )
}
