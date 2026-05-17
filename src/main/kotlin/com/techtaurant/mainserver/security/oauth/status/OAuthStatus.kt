package com.techtaurant.mainserver.security.oauth.status

import com.techtaurant.mainserver.common.status.StatusIfs

enum class OAuthStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    OAUTH_PROVIDER_NOT_SUPPORTED(400, 4001, "지원하지 않는 OAuth Provider입니다"),
    OAUTH_EMAIL_NOT_FOUND(400, 4002, "OAuth 응답에서 이메일을 찾을 수 없습니다"),
    OAUTH_AUTHENTICATION_FAILED(401, 4003, "OAuth 인증에 실패했습니다"),
    OAUTH_USER_INFO_LOAD_FAILED(500, 4004, "사용자 정보를 불러오는데 실패했습니다"),
    ;

    override fun getHttpStatusCode(): Int = httpStatusCode

    override fun getCustomStatusCode(): Int = customStatusCode

    override fun getDescription(): String = description
}
