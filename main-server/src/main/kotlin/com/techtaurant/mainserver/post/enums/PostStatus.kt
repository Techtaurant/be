package com.techtaurant.mainserver.post.enums

import com.techtaurant.mainserver.common.status.StatusIfs
import org.springframework.http.HttpStatus

enum class PostStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 3001, "게시물을 찾을 수 없습니다"),
    CATEGORY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST.value(), 3002, "카테고리 깊이는 최대 5단계까지 가능합니다"),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST.value(), 3003, "제목은 최대 200자까지 가능합니다"),
    INVALID_CATEGORY_PATH(HttpStatus.BAD_REQUEST.value(), 3004, "유효하지 않은 카테고리 경로입니다"),
    INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST.value(), 3005, "유효하지 않은 정렬 타입입니다"),
    ;

    override fun getHttpStatusCode(): Int {
        return this.httpStatusCode
    }

    override fun getCustomStatusCode(): Int {
        return this.customStatusCode
    }

    override fun getDescription(): String {
        return this.description
    }
}
