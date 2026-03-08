package com.techtaurant.mainserver.attachment.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

/**
 * S3 파일 I/O 작업 서비스.
 *
 * Presigned URL 생성, 파일 복사, 단건/배치 삭제를 담당한다.
 * 비즈니스 로직 없이 AWS S3 API 호출만 수행한다.
 */
@Service
class S3StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket-name}")
    private val bucketName: String,
) {
    /**
     * S3 PUT Presigned URL을 생성합니다.
     *
     * @param objectKey 업로드 대상 S3 오브젝트 키
     * @param contentType 파일 MIME 타입
     * @param expireMinutes URL 만료 시간 (분)
     * @return Presigned URL 문자열
     */
    fun generatePresignedUploadUrl(
        objectKey: String,
        contentType: String,
        expireMinutes: Long,
    ): String {
        val putObjectRequest =
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build()

        val presignRequest =
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expireMinutes))
                .putObjectRequest(putObjectRequest)
                .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    /**
     * S3 GET Presigned URL을 생성합니다.
     *
     * @param objectKey 다운로드 대상 S3 오브젝트 키
     * @param expireMinutes URL 만료 시간 (분)
     * @return Presigned URL 문자열
     */
    fun generatePresignedDownloadUrl(
        objectKey: String,
        expireMinutes: Long,
    ): String {
        val getObjectRequest =
            software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()

        val presignRequest =
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expireMinutes))
                .getObjectRequest(getObjectRequest)
                .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    /**
     * S3 오브젝트를 다른 키로 복사합니다.
     *
     * @param sourceKey 복사 원본 오브젝트 키
     * @param destinationKey 복사 대상 오브젝트 키
     */
    fun copyObject(
        sourceKey: String,
        destinationKey: String,
    ) {
        val request =
            CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build()

        s3Client.copyObject(request)
    }

    /**
     * S3 오브젝트를 삭제합니다.
     *
     * @param objectKey 삭제할 오브젝트 키
     */
    fun deleteObject(objectKey: String) {
        val request =
            DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()

        s3Client.deleteObject(request)
    }

    /**
     * S3 오브젝트 여러 개를 배치로 삭제합니다.
     *
     * @param objectKeys 삭제할 오브젝트 키 목록
     */
    fun deleteObjects(objectKeys: List<String>) {
        if (objectKeys.isEmpty()) return

        val identifiers =
            objectKeys.map { key ->
                ObjectIdentifier.builder().key(key).build()
            }

        val request =
            DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(identifiers).build())
                .build()

        s3Client.deleteObjects(request)
    }
}
