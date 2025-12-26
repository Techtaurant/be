package com.techtaurant.mainserver.infrastructure.out.crawler.dto

/**
 * 페이지 기반 링크 크롤링 검증 요청
 *
 * @param blogName 블로그 이름
 * @param baseUrl 크롤링할 기본 URL
 * @param startPage 시작 페이지 번호 (기본값: 0)
 * @param articlePattern 게시글 패턴 (기본값: 빈 문자열)
 * @param titleSelector 제목 선택자 (기본값: 빈 문자열)
 */
data class PageBaseLinkValidationRequest(
    val blogName: String,
    val baseUrl: String,
    val startPage: Int = 0,
    val articlePattern: String = "",
    val titleSelector: String = "",
)
