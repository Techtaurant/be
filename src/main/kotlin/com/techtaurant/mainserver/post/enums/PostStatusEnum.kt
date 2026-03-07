package com.techtaurant.mainserver.post.enums

/**
 * 게시물 상태를 나타내는 Enum
 *
 * DRAFT: 임시 저장 상태로 작성자만 조회 가능
 * PUBLISHED: 발행된 상태로 모든 사용자가 조회 가능
 * PRIVATE: 비공개 상태로 작성자만 조회 가능
 */
enum class PostStatusEnum {
    DRAFT,
    PUBLISHED,
    PRIVATE,
}
