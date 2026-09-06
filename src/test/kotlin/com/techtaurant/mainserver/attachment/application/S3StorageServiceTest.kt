package com.techtaurant.mainserver.attachment.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@DisplayName("S3 오브젝트 배치 삭제 단위 테스트")
class S3StorageServiceTest {
    private val s3Client: S3Client = mockk()
    private val s3Presigner: S3Presigner = mockk()

    private val s3StorageService =
        S3StorageService(
            s3Client = s3Client,
            s3Presigner = s3Presigner,
            bucketName = "test-bucket",
        )

    @Test
    @DisplayName("삭제할 키가 API 상한을 넘으면 요청을 나눠 보낸다")
    fun deleteObjects_moreKeysThanRequestLimit_splitsIntoMultipleRequests() {
        // given - DeleteObjects API 상한(1000)을 한 개 넘긴 키 목록
        val objectKeys = (1..1001).map { "tmp/$it/image.jpg" }
        val sentRequests = mutableListOf<DeleteObjectsRequest>()
        every { s3Client.deleteObjects(capture(sentRequests)) } returns DeleteObjectsResponse.builder().build()

        // when
        s3StorageService.deleteObjects(objectKeys)

        // then - 어느 요청도 상한을 넘지 않고, 키는 하나도 빠지지 않는다
        assertThat(sentRequests.map { it.delete().objects().size }).containsExactly(1000, 1)
        assertThat(sentRequests.flatMap { request -> request.delete().objects().map { it.key() } })
            .containsExactlyElementsOf(objectKeys)
    }

    @Test
    @DisplayName("삭제할 키가 상한 이하면 요청을 한 번만 보낸다")
    fun deleteObjects_keysWithinRequestLimit_sendsSingleRequest() {
        // given
        val objectKeys = (1..1000).map { "tmp/$it/image.jpg" }
        val sentRequests = mutableListOf<DeleteObjectsRequest>()
        every { s3Client.deleteObjects(capture(sentRequests)) } returns DeleteObjectsResponse.builder().build()

        // when
        s3StorageService.deleteObjects(objectKeys)

        // then
        assertThat(sentRequests.map { it.delete().objects().size }).containsExactly(1000)
    }

    @Test
    @DisplayName("삭제할 키가 없으면 S3를 호출하지 않는다")
    fun deleteObjects_emptyKeys_skipsRequest() {
        // when
        s3StorageService.deleteObjects(emptyList())

        // then - 빈 Delete 요청은 S3가 MalformedXML로 거절한다
        verify(exactly = 0) { s3Client.deleteObjects(any<DeleteObjectsRequest>()) }
    }
}
