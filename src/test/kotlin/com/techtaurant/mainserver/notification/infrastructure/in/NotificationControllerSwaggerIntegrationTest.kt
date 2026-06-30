package com.techtaurant.mainserver.notification.infrastructure.`in`

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import io.restassured.RestAssured
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("알림 Swagger 문서")
class NotificationControllerSwaggerIntegrationTest : IntegrationTest() {
    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("안읽은 알림 수 조회 성공 응답은 unreadCount 스키마를 노출한다")
    fun getMyUnreadNotificationCount_successResponse_exposesUnreadCountSchema() {
        val openApi = getOpenApiDocs()

        val successResponse =
            openApi
                .path("paths")
                .path("/api/notifications/unread-count")
                .path("get")
                .path("responses")
                .path("200")

        val responseSchema = resolveSuccessResponseSchema(openApi, successResponse)

        val properties = responseSchema.path("properties")
        val schemaDebug = responseSchema.toPrettyString()
        val dataSchema = resolveSchema(openApi = openApi, schema = properties.path("data"))
        val dataProperties = dataSchema.path("properties")

        assertTrue(properties.has("status"), "성공 응답 스키마에 status 필드가 있어야 한다: $schemaDebug")
        assertTrue(properties.has("data"), "성공 응답 스키마에 data 필드가 있어야 한다: $schemaDebug")
        assertTrue(properties.has("message"), "성공 응답 스키마에 message 필드가 있어야 한다: $schemaDebug")
        assertTrue(dataProperties.has("unreadCount"), "data 스키마에 unreadCount 필드가 있어야 한다: ${dataSchema.toPrettyString()}")
    }

    @Test
    @DisplayName("알림 스키마는 썸네일 필드를 노출하고 읽음 처리 전용 wrapper DTO를 만들지 않는다")
    fun notificationSchemas_exposeThumbnailUrlWithoutReadApiWrapper() {
        val openApi = getOpenApiDocs()

        val components = openApi.path("components").path("schemas")
        val itemSchema = components.path("NotificationListItemResponse")
        val itemProperties = itemSchema.path("properties")

        assertTrue(itemProperties.has("id"), "알림 항목 스키마에 id 필드가 있어야 한다: ${itemSchema.toPrettyString()}")
        assertTrue(itemProperties.has("thumbnailUrl"), "알림 항목 스키마에 thumbnailUrl 필드가 있어야 한다: ${itemSchema.toPrettyString()}")
        assertFalse(
            NotificationListItemResponse::class.memberProperties.single { it.name == "thumbnailUrl" }.returnType.isMarkedNullable,
            "알림 썸네일 URL은 기본 이미지 fallback이 있으므로 nullable이면 안 된다.",
        )
        assertFalse(
            itemProperties.path("thumbnailUrl").path("nullable").asBoolean(false),
            "알림 썸네일 URL OpenAPI 스키마는 nullable이면 안 된다: ${itemProperties.path("thumbnailUrl").toPrettyString()}",
        )
        assertTrue(itemProperties.has("arguments"), "알림 항목 스키마에 arguments 필드가 있어야 한다: ${itemSchema.toPrettyString()}")
        assertFalse(
            components.has("MarkNotificationsReadApiResponse"),
            "읽음 처리 성공 응답은 별도 wrapper DTO 스키마를 노출하지 않아야 한다: ${components.fieldNames().asSequence().toList()}",
        )
    }

    private fun getOpenApiDocs(): JsonNode =
        objectMapper.readTree(
            RestAssured
                .given()
                .`when`()
                .get("/v3/api-docs")
                .then()
                .statusCode(200)
                .extract()
                .asString(),
        )

    private fun resolveSchema(
        openApi: JsonNode,
        schema: JsonNode,
    ): JsonNode {
        val ref = schema.path("\$ref")
        if (!ref.isMissingNode && !ref.isNull) {
            val schemaName = ref.asText().substringAfterLast('/')
            return openApi.path("components").path("schemas").path(schemaName)
        }

        return schema
    }

    private fun resolveSuccessResponseSchema(
        openApi: JsonNode,
        successResponse: JsonNode,
    ): JsonNode {
        val content = successResponse.path("content")
        val mediaTypeNode =
            when {
                content.has("application/json") -> content.path("application/json")
                content.has("*/*") -> content.path("*/*")
                else -> content.elements().asSequence().firstOrNull() ?: content.path("application/json")
            }

        return resolveSchema(openApi, mediaTypeNode.path("schema"))
    }
}
