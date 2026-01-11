package com.techtaurant.mainserver.articlelink.dto

import com.techtaurant.mainserver.articlelink.enums.ArticleLinkType
import java.util.UUID

/**
 * 게시글 링크 검증 요청
 *
 * @param blogId 블로그 ID
 * @param baseUrl 크롤링할 기본 URL
 * @param startPage 시작 페이지 번호
 * @param articlePattern 게시글 패턴
 * @param titleSelector 제목 선택자
 * @param type 게시글 링크 타입
 */
data class ValidationRequest(
    val blogId: UUID,
    val baseUrl: String,
    val startPage: Int,
    val articlePattern: String,
    val titleSelector: String,
    val type: ArticleLinkType,
)
