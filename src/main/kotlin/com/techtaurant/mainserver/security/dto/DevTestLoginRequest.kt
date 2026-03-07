package com.techtaurant.mainserver.security.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * 개발 환경 테스트 로그인 요청 DTO
 *
 * @property identifier 테스트 사용자 식별자
 * @property password 개발 환경 고정 비밀번호 (dev-password)
 */
@Schema(description = "개발 환경 테스트 로그인 요청")
data class DevTestLoginRequest(
    @field:NotBlank(message = "식별자는 필수입니다")
    @field:Schema(description = "테스트 사용자 식별자", example = "dev-test-user")
    val identifier: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Schema(description = "개발 환경 비밀번호", example = "dev-password")
    val password: String,
)
