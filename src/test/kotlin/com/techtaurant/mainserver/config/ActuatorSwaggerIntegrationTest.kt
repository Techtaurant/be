package com.techtaurant.mainserver.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techtaurant.mainserver.base.IntegrationTest
import io.restassured.RestAssured
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("actuator Swagger 문서")
class ActuatorSwaggerIntegrationTest : IntegrationTest() {
    private val objectMapper = jacksonObjectMapper()

    @Test
    @DisplayName("actuator 그룹 문서는 health endpoint를 노출한다")
    fun actuatorGroup_exposesHealthEndpoint() {
        val paths = getOpenApiDocs(group = "actuator").path("paths")

        assertTrue(
            paths.has("/actuator/health"),
            "actuator 그룹 문서에 /actuator/health 경로가 있어야 한다: ${paths.fieldNames().asSequence().toList()}",
        )
    }

    @Test
    @DisplayName("actuator 그룹 문서는 health 외 다른 actuator endpoint(prometheus 등)를 노출하지 않는다")
    fun actuatorGroup_doesNotExposeNonHealthEndpoints() {
        val paths = getOpenApiDocs(group = "actuator").path("paths")

        val nonHealthPaths =
            paths
                .fieldNames()
                .asSequence()
                .filter { !it.startsWith("/actuator/health") }
                .toList()

        assertTrue(nonHealthPaths.isEmpty(), "actuator 그룹에는 health 경로만 있어야 한다: $nonHealthPaths")
    }

    @Test
    @DisplayName("기본 api 그룹 문서에는 actuator 경로가 포함되지 않는다")
    fun apiGroup_excludesActuatorPaths() {
        val paths = getOpenApiDocs(group = "api").path("paths")

        val actuatorPaths =
            paths
                .fieldNames()
                .asSequence()
                .filter { it.startsWith("/actuator") }
                .toList()

        assertTrue(actuatorPaths.isEmpty(), "api 그룹에는 actuator 경로가 없어야 한다: $actuatorPaths")
    }

    private fun getOpenApiDocs(group: String): JsonNode =
        objectMapper.readTree(
            RestAssured
                .given()
                .`when`()
                .get("/v3/api-docs/$group")
                .then()
                .statusCode(200)
                .extract()
                .asString(),
        )
}
