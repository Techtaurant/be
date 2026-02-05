package com.techtaurant.mainserver.base

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.springframework.http.MediaType

object TestHelper {
    private val objectMapper = ObjectMapper()

    fun RequestSpecification.setJsonContentType(): RequestSpecification {
        return this.contentType(MediaType.APPLICATION_JSON_VALUE)
    }

    fun RequestSpecification.withBody(body: Any): RequestSpecification {
        return this.body(objectMapper.writeValueAsString(body))
    }

    fun RequestSpecification.withCookie(
        name: String,
        value: String,
    ): RequestSpecification {
        return this.cookie(name, value)
    }

    fun RequestSpecification.withCookies(cookies: Map<String, String>): RequestSpecification {
        return this.cookies(cookies)
    }

    fun RequestSpecification.withBearerToken(token: String): RequestSpecification {
        return this.header("Authorization", "Bearer $token")
    }

    fun Response.extractPath(path: String): Any? {
        return this.jsonPath().get(path)
    }

    fun Response.extractString(path: String): String {
        return this.jsonPath().getString(path)
    }

    fun Response.extractInt(path: String): Int {
        return (this.jsonPath().get(path) as? Number)?.toInt() ?: 0
    }

    fun Response.extractList(path: String): List<*> {
        return this.jsonPath().getList<Any>(path)
    }

    fun assertResponseStatus(
        response: Response,
        expectedStatus: Int,
    ) {
        assert(response.statusCode == expectedStatus) {
            "Expected status $expectedStatus but got ${response.statusCode}. " +
                "Response body: ${response.body.asString()}"
        }
    }

    fun assertResponseContains(
        response: Response,
        expectedContent: String,
    ) {
        assert(response.body.asString().contains(expectedContent)) {
            "Response body does not contain expected content: $expectedContent"
        }
    }
}
