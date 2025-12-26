package com.techtaurant.mainserver.common.config

import com.techtaurant.mainserver.security.jwt.JwtConstants
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI? {
        // 실제 쿠키 이름을 여기에 넣으세요 (예: "accessToken", "JSESSIONID" 등)
        val cookieName = JwtConstants.ACCESS_TOKEN_COOKIE
        val securityName = "CookieAuth"

        val securityRequirement: SecurityRequirement = SecurityRequirement().addList(securityName)

        val components = Components().addSecuritySchemes(
            securityName, SecurityScheme()
                .name(cookieName) // 쿠키의 키(Key) 이름
                .type(SecurityScheme.Type.APIKEY) // 쿠키는 APIKEY 타입으로 설정
                .`in`(SecurityScheme.In.COOKIE) // 위치는 COOKIE
        )

        return OpenAPI()
            .components(components)
            .addSecurityItem(securityRequirement)
    }
}
