package com.techtaurant.mainserver.link.enums

import com.techtaurant.mainserver.common.status.StatusIfs
import org.springframework.http.HttpStatus

enum class LinkStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    LINK_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 6001, "링크를 찾을 수 없습니다"),
    LINK_CRAWL_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 6002, "링크 수집 배치를 찾을 수 없습니다"),
    INVALID_LINK_CRAWL_BATCH_CRON_EXPRESSION(HttpStatus.BAD_REQUEST.value(), 6004, "유효한 cron 표현식이 아닙니다"),
    INVALID_LINK_CURSOR(HttpStatus.BAD_REQUEST.value(), 6005, "유효한 링크 커서가 아닙니다"),
    LINK_CRAWL_BATCH_CREATED_AT_REQUIRED(HttpStatus.BAD_REQUEST.value(), 6006, "링크 수집 배치에서 생성일을 수집할 수 없습니다"),
    LINK_CRAWL_BATCH_NOT_CRAWLABLE(HttpStatus.BAD_REQUEST.value(), 6007, "링크 수집 배치를 크롤링할 수 없습니다"),
    CANNOT_MODIFY_LINK(HttpStatus.FORBIDDEN.value(), 6008, "첫 번째 등록자만 링크를 수정할 수 있습니다"),
    LINK_TITLE_REQUIRED(HttpStatus.BAD_REQUEST.value(), 6009, "링크 제목은 필수입니다"),
    LINK_URL_REQUIRED(HttpStatus.BAD_REQUEST.value(), 6010, "링크 URL은 필수입니다"),
    LINK_SUMMARY_REQUIRED(HttpStatus.BAD_REQUEST.value(), 6011, "링크 요약은 필수입니다"),
    INVALID_LINK_URL(HttpStatus.BAD_REQUEST.value(), 6012, "유효한 링크 URL이 아닙니다"),
    LINK_URL_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), 6013, "이미 등록된 링크 URL입니다"),
    CANNOT_UNSAVE_OWN_LINK(HttpStatus.FORBIDDEN.value(), 6014, "첫 번째 등록자는 링크 저장을 취소할 수 없습니다"),
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
