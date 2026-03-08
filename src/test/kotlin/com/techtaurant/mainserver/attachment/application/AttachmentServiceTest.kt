package com.techtaurant.mainserver.attachment.application

import com.techtaurant.mainserver.attachment.dto.PresignedUrlRequest
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.attachment.infrastructure.out.AttachmentRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class AttachmentServiceTest {
    private val attachmentRepository: AttachmentRepository = mockk()
    private val s3StorageService: S3StorageService = mockk()
    private val presignedUrlExpireMinutes = 10L

    private val attachmentService =
        AttachmentService(
            attachmentRepository = attachmentRepository,
            s3StorageService = s3StorageService,
            presignedUrlExpireMinutes = presignedUrlExpireMinutes,
        )

    private val postId = UUID.randomUUID()

    private fun makeAttachment(
        objectKey: String,
        status: AttachmentStatus = AttachmentStatus.CONFIRMED,
        referenceId: UUID? = postId,
    ): Attachment =
        Attachment(
            referenceId = referenceId,
            referenceType = AttachmentReferenceType.POST,
            objectKey = objectKey,
            status = status,
            originalFileName = objectKey.substringAfterLast("/"),
            contentType = "image/jpeg",
            fileSize = 1024L,
        ).apply { id = UUID.randomUUID() }

    @Nested
    @DisplayName("issuePresignedUploadUrl")
    inner class IssuePresignedUploadUrl {
        private val request =
            PresignedUrlRequest(
                fileName = "photo.jpg",
                contentType = "image/jpeg",
                fileSize = 1024L,
                referenceType = AttachmentReferenceType.POST,
            )

        @BeforeEach
        fun setUp() {
            val attachmentSlot = slot<Attachment>()
            every { attachmentRepository.save(capture(attachmentSlot)) } answers {
                attachmentSlot.captured.apply { id = UUID.randomUUID() }
            }
            every {
                s3StorageService.generatePresignedUploadUrl(any(), any(), any())
            } returns "https://s3.example.com/presigned"
        }

        @Test
        @DisplayName("TMP 상태의 Attachment를 생성하고 Presigned URL을 반환한다")
        fun issuePresignedUploadUrl_validRequest_returnsTmpAttachmentWithPresignedUrl() {
            // given & when
            val response = attachmentService.issuePresignedUploadUrl(request)

            // then
            assertThat(response.presignedUrl).isEqualTo("https://s3.example.com/presigned")
            assertThat(response.objectKey).startsWith("tmp/")
            assertThat(response.objectKey).endsWith("photo.jpg")
            assertThat(response.attachmentId).isNotNull()
        }

        @Test
        @DisplayName("objectKey는 tmp/{uuid}/{fileName} 형식으로 생성된다")
        fun issuePresignedUploadUrl_validRequest_generatesCorrectObjectKeyFormat() {
            // given & when
            val response = attachmentService.issuePresignedUploadUrl(request)

            // then
            val parts = response.objectKey.split("/")
            assertThat(parts).hasSize(3)
            assertThat(parts[0]).isEqualTo("tmp")
            assertThat(parts[2]).isEqualTo("photo.jpg")
        }

        @Test
        @DisplayName("Presigned URL 생성 시 요청의 contentType과 만료 시간을 전달한다")
        fun issuePresignedUploadUrl_validRequest_passesCorrectParamsToS3() {
            // given & when
            attachmentService.issuePresignedUploadUrl(request)

            // then
            verify {
                s3StorageService.generatePresignedUploadUrl(
                    objectKey = match { it.startsWith("tmp/") },
                    contentType = "image/jpeg",
                    expireMinutes = presignedUrlExpireMinutes,
                )
            }
        }
    }

    @Nested
    @DisplayName("confirmAttachments")
    inner class ConfirmAttachments {
        private val tmpKey = "tmp/${UUID.randomUUID()}/photo.jpg"

        @BeforeEach
        fun setUp() {
            every { s3StorageService.copyObject(any(), any()) } just runs
            every { s3StorageService.deleteObject(any()) } just runs
            every { attachmentRepository.saveAll(any<List<Attachment>>()) } answers { firstArg() }
        }

        @Test
        @DisplayName("빈 objectKeys 목록이면 빈 맵을 반환하고 S3 작업을 수행하지 않는다")
        fun confirmAttachments_emptyKeys_returnsEmptyMapWithoutS3Calls() {
            // given & when
            val result =
                attachmentService.confirmAttachments(
                    referenceId = postId,
                    referenceType = AttachmentReferenceType.POST,
                    objectKeys = emptyList(),
                )

            // then
            assertThat(result).isEmpty()
            verify(exactly = 0) { s3StorageService.copyObject(any(), any()) }
        }

        @Test
        @DisplayName("TMP 파일을 posts/{referenceId}/ 경로로 복사하고 원본을 삭제한다")
        fun confirmAttachments_tmpKey_copiesAndDeletesS3Object() {
            // given
            val tmpAttachment = makeAttachment(tmpKey, AttachmentStatus.TMP, referenceId = null)
            every {
                attachmentRepository.findAllByObjectKeyInAndStatus(listOf(tmpKey), AttachmentStatus.TMP)
            } returns listOf(tmpAttachment)

            // when
            val result =
                attachmentService.confirmAttachments(
                    referenceId = postId,
                    referenceType = AttachmentReferenceType.POST,
                    objectKeys = listOf(tmpKey),
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result.keys).containsExactly(tmpKey)
            val newKey = result[tmpKey]!!
            assertThat(newKey).startsWith("posts/$postId/")
            assertThat(newKey).endsWith("photo.jpg")

            verify { s3StorageService.copyObject(tmpKey, newKey) }
            verify { s3StorageService.deleteObject(tmpKey) }
        }

        @Test
        @DisplayName("Attachment의 status를 CONFIRMED로 변경하고 referenceId를 설정한다")
        fun confirmAttachments_tmpAttachment_updatesStatusAndReferenceId() {
            // given
            val tmpAttachment = makeAttachment(tmpKey, AttachmentStatus.TMP, referenceId = null)
            every {
                attachmentRepository.findAllByObjectKeyInAndStatus(listOf(tmpKey), AttachmentStatus.TMP)
            } returns listOf(tmpAttachment)

            // when
            attachmentService.confirmAttachments(
                referenceId = postId,
                referenceType = AttachmentReferenceType.POST,
                objectKeys = listOf(tmpKey),
            )

            // then
            assertThat(tmpAttachment.status).isEqualTo(AttachmentStatus.CONFIRMED)
            assertThat(tmpAttachment.referenceId).isEqualTo(postId)
            assertThat(tmpAttachment.referenceType).isEqualTo(AttachmentReferenceType.POST)
        }
    }

    @Nested
    @DisplayName("deleteAttachmentsByReference")
    inner class DeleteAttachmentsByReference {
        @Test
        @DisplayName("referenceId에 연결된 모든 첨부파일을 S3와 DB에서 삭제한다")
        fun deleteAttachmentsByReference_existingAttachments_deletesAllFromS3AndDb() {
            // given
            val attachment1 = makeAttachment("posts/$postId/uuid1/a.jpg")
            val attachment2 = makeAttachment("posts/$postId/uuid2/b.jpg")
            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(attachment1, attachment2)
            every { s3StorageService.deleteObjects(any()) } just runs
            every {
                attachmentRepository.deleteAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } just runs

            // when
            attachmentService.deleteAttachmentsByReference(postId, AttachmentReferenceType.POST)

            // then
            verify {
                s3StorageService.deleteObjects(
                    match { it.containsAll(listOf("posts/$postId/uuid1/a.jpg", "posts/$postId/uuid2/b.jpg")) },
                )
            }
            verify { attachmentRepository.deleteAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST) }
        }

        @Test
        @DisplayName("첨부파일이 없으면 S3 삭제와 DB 삭제를 수행하지 않는다")
        fun deleteAttachmentsByReference_noAttachments_skipsAllDeletion() {
            // given
            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns emptyList()

            // when
            attachmentService.deleteAttachmentsByReference(postId, AttachmentReferenceType.POST)

            // then
            verify(exactly = 0) { s3StorageService.deleteObjects(any()) }
            verify(exactly = 0) {
                attachmentRepository.deleteAllByReferenceIdAndReferenceType(any(), any())
            }
        }
    }

    @Nested
    @DisplayName("deleteOrphanedAttachments")
    inner class DeleteOrphanedAttachments {
        @Test
        @DisplayName("keepObjectKeys에 없는 첨부파일을 S3와 DB에서 삭제한다")
        fun deleteOrphanedAttachments_orphanExists_deletesOrphansOnly() {
            // given
            val keepKey = "posts/$postId/uuid1/keep.jpg"
            val orphanKey = "posts/$postId/uuid2/orphan.jpg"
            val keepAttachment = makeAttachment(keepKey)
            val orphanAttachment = makeAttachment(orphanKey)

            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(keepAttachment, orphanAttachment)
            every { s3StorageService.deleteObjects(any()) } just runs
            every { attachmentRepository.deleteAll(any<List<Attachment>>()) } just runs

            // when
            attachmentService.deleteOrphanedAttachments(postId, AttachmentReferenceType.POST, listOf(keepKey))

            // then
            verify { s3StorageService.deleteObjects(listOf(orphanKey)) }
            verify { attachmentRepository.deleteAll(listOf(orphanAttachment)) }
        }

        @Test
        @DisplayName("고아 파일이 없으면 S3 삭제를 수행하지 않는다")
        fun deleteOrphanedAttachments_noOrphans_skipsDeletion() {
            // given
            val keepKey = "posts/$postId/uuid1/keep.jpg"
            val keepAttachment = makeAttachment(keepKey)

            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(keepAttachment)

            // when
            attachmentService.deleteOrphanedAttachments(postId, AttachmentReferenceType.POST, listOf(keepKey))

            // then
            verify(exactly = 0) { s3StorageService.deleteObjects(any()) }
        }
    }

    @Nested
    @DisplayName("generatePresignedDownloadUrlMap")
    inner class GeneratePresignedDownloadUrlMap {
        @Test
        @DisplayName("CONFIRMED 첨부파일의 objectKey → presigned GET URL 맵을 반환한다")
        fun generatePresignedDownloadUrlMap_confirmedAttachments_returnsUrlMap() {
            // given
            val key1 = "posts/$postId/uuid1/a.jpg"
            val key2 = "posts/$postId/uuid2/b.jpg"
            val confirmed1 = makeAttachment(key1, AttachmentStatus.CONFIRMED)
            val confirmed2 = makeAttachment(key2, AttachmentStatus.CONFIRMED)

            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(confirmed1, confirmed2)
            every { s3StorageService.generatePresignedDownloadUrl(key1, presignedUrlExpireMinutes) } returns "https://url1"
            every { s3StorageService.generatePresignedDownloadUrl(key2, presignedUrlExpireMinutes) } returns "https://url2"

            // when
            val result = attachmentService.generatePresignedDownloadUrlMap(postId, AttachmentReferenceType.POST)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[key1]).isEqualTo("https://url1")
            assertThat(result[key2]).isEqualTo("https://url2")
        }

        @Test
        @DisplayName("TMP 상태의 첨부파일은 URL 맵에 포함하지 않는다")
        fun generatePresignedDownloadUrlMap_tmpAttachment_excludedFromMap() {
            // given
            val confirmedKey = "posts/$postId/uuid1/confirmed.jpg"
            val tmpKey = "tmp/${UUID.randomUUID()}/tmp.jpg"
            val confirmed = makeAttachment(confirmedKey, AttachmentStatus.CONFIRMED)
            val tmp = makeAttachment(tmpKey, AttachmentStatus.TMP)

            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(confirmed, tmp)
            every { s3StorageService.generatePresignedDownloadUrl(confirmedKey, presignedUrlExpireMinutes) } returns "https://url"

            // when
            val result = attachmentService.generatePresignedDownloadUrlMap(postId, AttachmentReferenceType.POST)

            // then
            assertThat(result).hasSize(1)
            assertThat(result).containsKey(confirmedKey)
            assertThat(result).doesNotContainKey(tmpKey)
        }

        @Test
        @DisplayName("첨부파일이 없으면 빈 맵을 반환한다")
        fun generatePresignedDownloadUrlMap_noAttachments_returnsEmptyMap() {
            // given
            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns emptyList()

            // when
            val result = attachmentService.generatePresignedDownloadUrlMap(postId, AttachmentReferenceType.POST)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 0) { s3StorageService.generatePresignedDownloadUrl(any(), any()) }
        }
    }

    @Nested
    @DisplayName("getConfirmedAttachments")
    inner class GetConfirmedAttachments {
        @Test
        @DisplayName("CONFIRMED 상태의 첨부파일만 반환한다")
        fun getConfirmedAttachments_mixedStatuses_returnsOnlyConfirmed() {
            // given
            val confirmed = makeAttachment("posts/$postId/uuid1/a.jpg", AttachmentStatus.CONFIRMED)
            val tmp = makeAttachment("tmp/${UUID.randomUUID()}/b.jpg", AttachmentStatus.TMP)

            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns listOf(confirmed, tmp)

            // when
            val result = attachmentService.getConfirmedAttachments(postId, AttachmentReferenceType.POST)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(AttachmentStatus.CONFIRMED)
            assertThat(result[0].objectKey).isEqualTo("posts/$postId/uuid1/a.jpg")
        }

        @Test
        @DisplayName("첨부파일이 없으면 빈 목록을 반환한다")
        fun getConfirmedAttachments_noAttachments_returnsEmptyList() {
            // given
            every {
                attachmentRepository.findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
            } returns emptyList()

            // when
            val result = attachmentService.getConfirmedAttachments(postId, AttachmentReferenceType.POST)

            // then
            assertThat(result).isEmpty()
        }
    }
}
