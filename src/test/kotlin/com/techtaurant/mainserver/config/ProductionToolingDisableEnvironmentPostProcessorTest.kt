package com.techtaurant.mainserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.mock.env.MockEnvironment

class ProductionToolingDisableEnvironmentPostProcessorTest {
    private val postProcessor = ProductionToolingDisableEnvironmentPostProcessor()

    @Test
    @DisplayName("app.environment가 prod이면 Swagger와 p6spy SQL logging을 비활성화한다")
    fun postProcessEnvironment_prodEnvironment_disablesSwaggerAndP6spyLogging() {
        // given
        val environment = MockEnvironment().withProperty("app.environment", " prod ")

        // when
        postProcessor.postProcessEnvironment(environment, SpringApplication())

        // then
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false")
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false")
        assertThat(environment.getProperty("decorator.datasource.p6spy.enable-logging")).isEqualTo("false")
    }

    @Test
    @DisplayName("app.environment가 prod가 아니면 기존 Swagger와 p6spy 설정을 유지한다")
    fun postProcessEnvironment_nonProdEnvironment_keepsExistingToolingConfiguration() {
        // given
        val environment =
            MockEnvironment()
                .withProperty("app.environment", "dev")
                .withProperty("springdoc.api-docs.enabled", "true")
                .withProperty("springdoc.swagger-ui.enabled", "true")
                .withProperty("decorator.datasource.p6spy.enable-logging", "true")

        // when
        postProcessor.postProcessEnvironment(environment, SpringApplication())

        // then
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("true")
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true")
        assertThat(environment.getProperty("decorator.datasource.p6spy.enable-logging")).isEqualTo("true")
    }

    @Test
    @DisplayName("spring.factories에 등록된 후처리기가 prod 환경에서 자동으로 Swagger와 p6spy SQL logging을 비활성화한다")
    fun springApplication_prodEnvironment_disablesSwaggerAndP6spyLoggingThroughRegisteredPostProcessor() {
        val context =
            SpringApplicationBuilder(TestApplication::class.java)
                .properties(
                    "spring.config.location=optional:classpath:/empty-application.yml",
                    "app.environment=prod",
                )
                .web(WebApplicationType.NONE)
                .run()

        try {
            assertThat(context.environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false")
            assertThat(context.environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false")
            assertThat(context.environment.getProperty("decorator.datasource.p6spy.enable-logging")).isEqualTo("false")
        } finally {
            context.close()
        }
    }

    @Configuration(proxyBeanMethods = false)
    private class TestApplication
}
