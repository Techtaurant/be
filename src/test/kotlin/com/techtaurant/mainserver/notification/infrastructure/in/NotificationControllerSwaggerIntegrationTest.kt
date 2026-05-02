package com.techtaurant.mainserver.notification.infrastructure.`in`

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
    @DisplayName("알림 스키마는 썸네일 필드를 노출하고 읽음 처리 전용 wrapper DTO를 만들지 않는다")
    fun notificationSchemas_exposeThumbnailUrlWithoutReadApiWrapper() {
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
}
