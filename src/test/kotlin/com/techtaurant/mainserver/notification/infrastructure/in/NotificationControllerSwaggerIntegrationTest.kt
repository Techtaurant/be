package com.techtaurant.mainserver.notification.infrastructure.`in`

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techtaurant.mainserver.base.IntegrationTest
import io.restassured.RestAssured
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("알림 Swagger 문서")
class NotificationControllerSwaggerIntegrationTest : IntegrationTest() {
    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("알림 다건 읽음 처리 성공 응답은 읽음 처리된 알림 목록 스키마를 노출한다")
    fun markNotificationsRead_successResponse_exposesReadNotificationsSchema() {
        val openApi =
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

        val successResponse =
            openApi
                .path("paths")
                .path("/api/notifications/read")
                .path("patch")
                .path("responses")
                .path("200")

        val responseNode =
            successResponse
                .path("content")
                .path("application/json")

        val responseSchema =
            resolveSchema(
                openApi = openApi,
                schema =
                    responseNode
                        .path("schema"),
            )

        val properties = responseSchema.path("properties")
        val schemaDebug = responseSchema.toPrettyString()
        val dataSchema = resolveSchema(openApi = openApi, schema = properties.path("data"))
        val dataProperties = dataSchema.path("properties")
        val notificationsSchema = resolveSchema(openApi = openApi, schema = dataProperties.path("notifications"))
        val itemSchema = resolveSchema(openApi = openApi, schema = notificationsSchema.path("items"))
        val itemProperties = itemSchema.path("properties")

        assertTrue(properties.has("status"), "성공 응답 스키마에 status 필드가 있어야 한다: $schemaDebug")
        assertTrue(properties.has("data"), "성공 응답 스키마에 data 필드가 있어야 한다: $schemaDebug")
        assertTrue(properties.has("message"), "성공 응답 스키마에 message 필드가 있어야 한다: $schemaDebug")
        assertTrue(dataProperties.has("notifications"), "data 스키마에 notifications 필드가 있어야 한다: ${dataSchema.toPrettyString()}")
        assertTrue(notificationsSchema.path("type").asText() == "array", "notifications 필드는 배열이어야 한다: ${notificationsSchema.toPrettyString()}")
        assertTrue(itemProperties.has("id"), "알림 항목 스키마에 id 필드가 있어야 한다: ${itemSchema.toPrettyString()}")
        assertTrue(itemProperties.has("arguments"), "알림 항목 스키마에 arguments 필드가 있어야 한다: ${itemSchema.toPrettyString()}")
    }

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
}
