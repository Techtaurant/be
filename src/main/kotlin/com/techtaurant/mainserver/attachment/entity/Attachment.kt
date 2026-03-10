package com.techtaurant.mainserver.attachment.entity

import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * 첨부파일 엔티티
 *
 * referenceId + referenceType 으로 여러 도메인의 파일을 범용으로 관리한다.
 * 업로드 직후에는 TMP 상태로 S3 tmp/ 경로에 저장되며,
 * 게시물 publish 시 CONFIRMED 상태로 전환되고 posts/{referenceId}/ 경로로 이동된다.
 *
 * @property referenceId 연관 도메인 PK (TMP 상태에서는 null 가능)
 * @property referenceType 연관 도메인 타입 (POST 등)
 * @property objectKey S3 오브젝트 키 (tmp/{uuid}/{fileName} 또는 posts/{postId}/{uuid}/{fileName})
 * @property status 파일 상태 (TMP: 임시, CONFIRMED: 확정)
 * @property originalFileName 원본 파일명
 * @property contentType MIME 타입 (예: image/jpeg)
 * @property fileSize 파일 크기 (bytes)
 */
@Entity
@Table(
    name = "attachments",
    indexes = [
        Index(name = "idx_attachments_reference", columnList = "reference_id, reference_type"),
        Index(name = "idx_attachments_status", columnList = "status"),
    ],
)
class Attachment(
    @Column(name = "reference_id")
    var referenceId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reference_type", nullable = false, columnDefinition = "attachment_reference_type")
    var referenceType: AttachmentReferenceType,
    @Column(name = "object_key", nullable = false, length = 500)
    var objectKey: String,
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "attachment_status")
    var status: AttachmentStatus,
    @Column(name = "original_file_name", nullable = false, length = 255)
    var originalFileName: String,
    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,
    @Column(name = "file_size", nullable = false)
    var fileSize: Long,
) : EntityBase()
