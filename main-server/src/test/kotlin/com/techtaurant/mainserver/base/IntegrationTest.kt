package com.techtaurant.mainserver.base

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest {
    @LocalServerPort
    protected var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        RestAssured.baseURI = "http://localhost"
    }

    protected fun getBaseUrl(): String = "http://localhost:$port"

    protected fun configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    companion object {
        private val postgresContainer =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("techtaurant_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))

        private val redisContainer =
            GenericContainer("redis:7-alpine")
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))

        init {
            postgresContainer.start()
            redisContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
        }
    }
}
