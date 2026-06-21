package com.techtaurant.mainserver.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "사용자 토큰 발급 요청")
data class CreateUserTokenRequest(
    @field:NotBlank(message = "토큰 이름은 필수입니다")
    @field:Size(max = 100, message = "토큰 이름은 최대 100자까지 가능합니다")
    @field:Schema(description = "관리자가 식별할 토큰 이름", example = "토스 기술블로그 수집 봇")
    val name: String,
)
