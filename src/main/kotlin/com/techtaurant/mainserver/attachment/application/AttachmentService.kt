package com.techtaurant.mainserver.attachment.application

import com.techtaurant.mainserver.attachment.dto.PresignedUrlRequest
import com.techtaurant.mainserver.attachment.dto.PresignedUrlResponse
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.attachment.infrastructure.out.AttachmentRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 첨부파일 비즈니스 로직 서비스.
 *
 * Presigned URL 발급, TMP → CONFIRMED 전환(S3 파일 이동 포함),
 * 첨부파일 삭제를 담당한다.
 */
@Service
class AttachmentService(
    private val attachmentRepository: AttachmentRepository,
    private val s3StorageService: S3StorageService,
    @param:Value("\${aws.s3.presigned-url-expire-minutes}")
    private val presignedUrlExpireMinutes: Long,
) {
    /**
     * S3 PUT Presigned URL을 발급하고 TMP 상태의 Attachment 레코드를 생성합니다.
     *
     * @param request Presigned URL 발급 요청 (파일명, MIME 타입, 파일 크기, 도메인 타입)
     * @return Presigned URL, objectKey, attachmentId 응답
     */
    @Transactional
    fun issuePresignedUploadUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val uniqueId = UUID.randomUUID()
        val objectKey = "tmp/$uniqueId/${request.fileName}"

        val attachment =
            attachmentRepository.save(
                Attachment(
                    referenceId = null,
                    referenceType = request.referenceType,
                    objectKey = objectKey,
                    status = AttachmentStatus.TMP,
                    originalFileName = request.fileName,
                    contentType = request.contentType,
                    fileSize = request.fileSize,
                ),
            )

        val presignedUrl =
            s3StorageService.generatePresignedUploadUrl(
                objectKey = objectKey,
                contentType = request.contentType,
                expireMinutes = presignedUrlExpireMinutes,
            )

        return PresignedUrlResponse.from(attachment, presignedUrl)
    }

    /**
     * objectKeys에 해당하는 TMP Attachment를 CONFIRMED 상태로 전환합니다.
     * S3 파일을 tmp/ 경로에서 posts/{referenceId}/ 경로로 이동합니다.
     *
     * @param referenceId 연관 도메인 PK (게시물 ID 등)
     * @param referenceType 연관 도메인 타입
     * @param objectKeys 확정할 S3 objectKey 목록 (content에서 파싱한 값)
     * @return 이동된 새 objectKey 목록 (content URL 교체용)
     */
    @Transactional
    fun confirmAttachments(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
        objectKeys: List<String>,
    ): Map<String, String> {
        if (objectKeys.isEmpty()) return emptyMap()

        val tmpObjectKeyToNewKey = mutableMapOf<String, String>()

        objectKeys.forEach { tmpObjectKey ->
            val uniqueId = UUID.randomUUID()
            val fileName = tmpObjectKey.substringAfterLast("/")
            val newObjectKey = "posts/$referenceId/$uniqueId/$fileName"

            s3StorageService.copyObject(
                sourceKey = tmpObjectKey,
                destinationKey = newObjectKey,
            )
            s3StorageService.deleteObject(tmpObjectKey)

            tmpObjectKeyToNewKey[tmpObjectKey] = newObjectKey
        }

        val tmps = attachmentRepository.findAllByObjectKeyInAndStatus(objectKeys, AttachmentStatus.TMP)

        tmps.forEach { attachment ->
            val newKey = tmpObjectKeyToNewKey[attachment.objectKey] ?: return@forEach
            attachment.objectKey = newKey
            attachment.status = AttachmentStatus.CONFIRMED
            attachment.referenceId = referenceId
            attachment.referenceType = referenceType
        }

        attachmentRepository.saveAll(tmps)
        return tmpObjectKeyToNewKey
    }

    /**
     * referenceId에 연결된 모든 첨부파일을 S3와 DB에서 삭제합니다.
     *
     * @param referenceId 연관 도메인 PK
     * @param referenceType 연관 도메인 타입
     */
    @Transactional
    fun deleteAttachmentsByReference(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
    ) {
        val attachments = attachmentRepository.findAllByReferenceIdAndReferenceType(referenceId, referenceType)
        if (attachments.isEmpty()) return

        s3StorageService.deleteObjects(attachments.map { it.objectKey })
        attachmentRepository.deleteAllByReferenceIdAndReferenceType(referenceId, referenceType)
    }

    /**
     * keepObjectKeys에 포함되지 않는 첨부파일을 S3와 DB에서 삭제합니다.
     * 게시물 수정 시 content에서 제거된 이미지 정리에 사용됩니다.
     *
     * @param referenceId 연관 도메인 PK
     * @param referenceType 연관 도메인 타입
     * @param keepObjectKeys 유지할 objectKey 목록
     */
    @Transactional
    fun deleteOrphanedAttachments(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
        keepObjectKeys: List<String>,
    ) {
        val attachments = attachmentRepository.findAllByReferenceIdAndReferenceType(referenceId, referenceType)
        val orphaned = attachments.filter { it.objectKey !in keepObjectKeys }
        if (orphaned.isEmpty()) return

        s3StorageService.deleteObjects(orphaned.map { it.objectKey })
        attachmentRepository.deleteAll(orphaned)
    }

    /**
     * 특정 referenceId에 연결된 CONFIRMED 첨부파일 목록을 조회합니다.
     *
     * @param referenceId 연관 도메인 PK
     * @param referenceType 연관 도메인 타입
     * @return 첨부파일 목록
     */
    @Transactional(readOnly = true)
    fun getConfirmedAttachments(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
    ): List<Attachment> =
        attachmentRepository.findAllByReferenceIdAndReferenceType(referenceId, referenceType)
            .filter { it.status == AttachmentStatus.CONFIRMED }

    /**
     * referenceId에 연결된 CONFIRMED 첨부파일의 objectKey → presigned GET URL 맵을 반환합니다.
     * content 내 object key를 접근 가능한 URL로 교체할 때 사용합니다.
     *
     * @param referenceId 연관 도메인 PK
     * @param referenceType 연관 도메인 타입
     * @return objectKey → presigned GET URL 맵
     */
    @Transactional(readOnly = true)
    fun generatePresignedDownloadUrlMap(
        referenceId: UUID,
        referenceType: AttachmentReferenceType,
    ): Map<String, String> {
        val attachments =
            attachmentRepository.findAllByReferenceIdAndReferenceType(referenceId, referenceType)
                .filter { it.status == AttachmentStatus.CONFIRMED }

        return attachments.associate { attachment ->
            attachment.objectKey to
                s3StorageService.generatePresignedDownloadUrl(
                    objectKey = attachment.objectKey,
                    expireMinutes = presignedUrlExpireMinutes,
                )
        }
    }

    /**
     * referenceId 목록에 연결된 CONFIRMED 첨부파일을 배치 조회합니다.
     * 게시물 목록 조회 시 썸네일 계산에 사용됩니다.
     *
     * @param referenceIds 연관 도메인 PK 목록
     * @param referenceType 연관 도메인 타입
     * @return referenceId → 첨부파일 목록 맵
     */
    @Transactional(readOnly = true)
    fun getConfirmedAttachmentsByReferenceIds(
        referenceIds: List<UUID>,
        referenceType: AttachmentReferenceType,
    ): Map<UUID, List<Attachment>> =
        attachmentRepository
            .findAllByReferenceIdInAndReferenceType(referenceIds, referenceType)
            .filter { it.status == AttachmentStatus.CONFIRMED }
            .groupBy { it.referenceId!! }
}
