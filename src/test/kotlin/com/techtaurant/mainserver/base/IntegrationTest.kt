package com.techtaurant.mainserver.base

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        RestAssured.port = port
        RestAssured.basePath = ""
        RestAssured.baseURI = "http://localhost"
    }

    protected fun getBaseUrl(): String = "http://localhost:$port"

    protected fun configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    private fun cleanDatabase() {
        val tableNames =
            jdbcTemplate.queryForList(
                """
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename <> 'flyway_schema_history'
                """.trimIndent(),
                String::class.java,
            )

        if (tableNames.isEmpty()) {
            return
        }

        val truncateQuery = tableNames.joinToString(prefix = "TRUNCATE TABLE ", separator = ", ", postfix = " RESTART IDENTITY CASCADE")
        jdbcTemplate.execute(truncateQuery)
    }

    companion object {
        private val postgresContainer =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("techtaurant_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))

        init {
            postgresContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
        }
    }
}
