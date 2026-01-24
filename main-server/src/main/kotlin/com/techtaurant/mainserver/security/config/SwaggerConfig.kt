package com.techtaurant.mainserver.security.config

import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.jwt.JwtConstants
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger OpenAPI 설정
 *
 * Cookie 기반 인증을 사용하여 /api/ 엔드포인트 테스트 가능
 * OAuth 로그인 후 브라우저에 설정된 accessToken cookie를 통해 인증됨
 */
@Configuration
class SwaggerConfig {

    companion object {
        private const val COOKIE_AUTH_SCHEME = "cookieAuth"
    }

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
            .components(securityComponents())
            .addSecurityItem(SecurityRequirement().addList(COOKIE_AUTH_SCHEME))
    }

    @Bean
    fun openApiCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.paths.forEach { (path, pathItem) ->
                if (path.startsWith(SecurityConstants.OPEN_API_PREFIX)) {
                    pathItem.readOperations().forEach { operation ->
                        operation.security = emptyList()
                    }
                }
            }
        }
    }

    private fun apiInfo(): Info {
        return Info()
            .title("Techtaurant API")
            .description(buildDescription())
            .version("1.0.0")
    }

    private fun buildDescription(): String {
        return "Techtaurant Main Server API Documentation\n\n" +
            "## 인증 방법\n" +
            "`/api/` 엔드포인트는 로그인이 필요합니다.\n\n" +
            "1. [Google OAuth 로그인](/oauth2/authorization/google)으로 로그인\n" +
            "2. 로그인 성공 시 브라우저에 `accessToken` cookie가 설정됨\n" +
            "3. Swagger UI에서 `/api/` 엔드포인트 테스트 가능\n\n" +
            "> Note: 브라우저에서 Swagger UI를 사용할 때 cookie가 자동으로 전송됩니다."
    }

    /**
     * Cookie 기반 인증 Security Scheme 설정
     * accessToken cookie를 사용하여 API 인증
     */
    private fun securityComponents(): Components {
        return Components()
            .addSecuritySchemes(
                COOKIE_AUTH_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name(JwtConstants.ACCESS_TOKEN_COOKIE)
                    .description("OAuth 로그인 후 자동으로 설정되는 accessToken cookie")
            )
    }
}
