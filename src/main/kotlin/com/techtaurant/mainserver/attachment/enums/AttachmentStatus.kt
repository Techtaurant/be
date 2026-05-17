package com.techtaurant.mainserver.attachment.enums

enum class AttachmentStatus {
    /** S3 tmp/ 경로에 임시 저장된 상태. 게시물 publish 전까지 유지된다. */
    TMP,

    /** 게시물 publish 시 posts/{postId}/ 경로로 이동 완료된 상태. */
    CONFIRMED,
}
