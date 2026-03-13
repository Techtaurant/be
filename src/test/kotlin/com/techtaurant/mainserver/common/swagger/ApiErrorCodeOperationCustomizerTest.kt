package com.techtaurant.mainserver.common.swagger

import com.techtaurant.mainserver.common.dto.ValidationErrorResponse
import com.techtaurant.mainserver.common.status.StatusIfs
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.responses.ApiResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod

class ApiErrorCodeOperationCustomizerTest {
    private val customizer = ApiErrorCodeOperationCustomizer()
    private val controller = TestController()

    @Test
    @DisplayName("400 에러 응답에는 비즈니스 에러와 검증 에러 스키마를 함께 설정한다")
    fun customize_withBadRequestAnnotation_addsComposedSchemaAndValidationExample() {
        // Given
        val operation = Operation()
        val handlerMethod = createHandlerMethod("badRequest")

        // When
        val customized = customizer.customize(operation, handlerMethod)

        // Then
        val response = requireNotNull(customized.responses["400"])
        assertThat(response.description).isEqualTo("입력값 검증 실패 또는 비즈니스 규칙 위반")

        val mediaType = requireNotNull(response.content["application/json"])
        assertThat(mediaType.examples).containsKeys("잘못된 요청", "중복 요청", "Validation 에러")

        val businessErrorExample = requireNotNull(mediaType.examples["잘못된 요청"])
        assertThat(businessErrorExample.value).isEqualTo(
            linkedMapOf(
                "status" to 1400,
                "data" to null,
                "message" to "잘못된 요청",
            ),
        )

        val validationExample = requireNotNull(mediaType.examples["Validation 에러"])
        assertThat(validationExample.value).isEqualTo(
            linkedMapOf(
                "status" to 400,
                "data" to ValidationErrorResponse(mapOf("field" to "에러 메시지")),
                "message" to "Wrong Request",
            ),
        )

        val schema = requireNotNull(mediaType.schema)
        assertThat(schema).isInstanceOf(ComposedSchema::class.java)

        val composedSchema = schema as ComposedSchema
        assertThat(composedSchema.oneOf).hasSize(2)

        val businessSchema = composedSchema.oneOf[0] as ObjectSchema
        assertThat(businessSchema.properties).containsKeys("status", "data", "message")

        val validationSchema = composedSchema.oneOf[1] as ObjectSchema
        val validationDataSchema = validationSchema.properties["data"] as ObjectSchema
        val errorsSchema = validationDataSchema.properties["errors"] as MapSchema
        assertThat(errorsSchema.additionalProperties).isNotNull
    }

    @Test
    @DisplayName("일반 에러 응답에는 단일 비즈니스 에러 스키마를 설정한다")
    fun customize_withNotFoundAnnotation_addsBusinessErrorSchema() {
        // Given
        val operation = Operation()
        val handlerMethod = createHandlerMethod("notFound")

        // When
        val customized = customizer.customize(operation, handlerMethod)

        // Then
        val response = requireNotNull(customized.responses["404"])
        assertThat(response.description).isEqualTo("리소스 미존재")

        val mediaType = requireNotNull(response.content["application/json"])
        assertThat(mediaType.examples).containsOnlyKeys("리소스 없음")
        assertThat(mediaType.schema).isInstanceOf(ObjectSchema::class.java)
        assertThat(mediaType.schema).isNotInstanceOf(ComposedSchema::class.java)

        val schema = requireNotNull(mediaType.schema) as ObjectSchema
        assertThat(schema.properties).containsKeys("status", "data", "message")
    }

    @Test
    @DisplayName("기존 응답 설명과 스키마가 있으면 덮어쓰지 않고 예시만 추가한다")
    fun customize_withExistingResponse_preservesDescriptionAndSchema() {
        // Given
        val existingSchema = ObjectSchema().description("기존 스키마")
        val existingMediaType = MediaType().schema(existingSchema)
        val existingResponse =
            io.swagger.v3.oas.models.responses.ApiResponse()
                .description("기존 설명")
                .content(Content().addMediaType("application/json", existingMediaType))
        val operation =
            Operation().responses(
                ApiResponses().addApiResponse("403", existingResponse),
            )
        val handlerMethod = createHandlerMethod("forbidden")

        // When
        val customized = customizer.customize(operation, handlerMethod)

        // Then
        val response = requireNotNull(customized.responses["403"])
        assertThat(response.description).isEqualTo("기존 설명")

        val mediaType = requireNotNull(response.content["application/json"])
        assertThat(mediaType.schema).isSameAs(existingSchema)
        assertThat(mediaType.examples).containsKey("권한 없음")
    }

    @Test
    @DisplayName("에러 코드 어노테이션이 없으면 기존 Operation을 그대로 반환한다")
    fun customize_withoutAnnotation_returnsOriginalOperation() {
        // Given
        val operation = Operation()
        val handlerMethod = createHandlerMethod("withoutAnnotation")

        // When
        val customized = customizer.customize(operation, handlerMethod)

        // Then
        assertThat(customized).isSameAs(operation)
        assertThat(customized.responses).isNull()
    }

    private fun createHandlerMethod(methodName: String): HandlerMethod =
        HandlerMethod(controller, controller::class.java.getMethod(methodName))

    private class TestController {
        @ApiErrorCodeResponse(statusType = TestStatus::class, values = ["BAD_REQUEST_ONE", "BAD_REQUEST_TWO"])
        fun badRequest() {
        }

        @ApiErrorCodeResponse(statusType = TestStatus::class, values = ["NOT_FOUND"])
        fun notFound() {
        }

        @ApiErrorCodeResponse(statusType = TestStatus::class, values = ["FORBIDDEN"])
        fun forbidden() {
        }

        fun withoutAnnotation() {
        }
    }

    private enum class TestStatus(
        private val httpStatusCode: Int,
        private val customStatusCode: Int,
        private val description: String,
    ) : StatusIfs {
        BAD_REQUEST_ONE(400, 1400, "잘못된 요청"),
        BAD_REQUEST_TWO(400, 1401, "중복 요청"),
        FORBIDDEN(403, 1403, "권한 없음"),
        NOT_FOUND(404, 1404, "리소스 없음"),
        ;

        override fun getHttpStatusCode(): Int = httpStatusCode

        override fun getCustomStatusCode(): Int = customStatusCode

        override fun getDescription(): String = description
    }
}
