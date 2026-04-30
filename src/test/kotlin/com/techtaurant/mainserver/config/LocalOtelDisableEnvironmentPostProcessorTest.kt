package com.techtaurant.mainserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.mock.env.MockEnvironment

class LocalOtelDisableEnvironmentPostProcessorTest {
    private val postProcessor = LocalOtelDisableEnvironmentPostProcessor()

    @Test
    @DisplayName("app.environment가 local이면 OTEL SDK와 exporter를 비활성화한다")
    fun postProcessEnvironment_localEnvironment_disablesOtel() {
        // given
        val environment = MockEnvironment().withProperty("app.environment", "local")

        // when
        postProcessor.postProcessEnvironment(environment, SpringApplication())

        // then
        assertThat(environment.getProperty("otel.sdk.disabled")).isEqualTo("true")
        assertThat(environment.getProperty("otel.traces.exporter")).isEqualTo("none")
        assertThat(environment.getProperty("otel.logs.exporter")).isEqualTo("none")
    }

    @Test
    @DisplayName("app.environment가 local이 아니면 기존 OTEL 설정을 유지한다")
    fun postProcessEnvironment_nonLocalEnvironment_keepsExistingOtelConfiguration() {
        // given
        val environment =
            MockEnvironment()
                .withProperty("app.environment", "dev")
                .withProperty("otel.sdk.disabled", "false")
                .withProperty("otel.traces.exporter", "otlp")
                .withProperty("otel.logs.exporter", "otlp")

        // when
        postProcessor.postProcessEnvironment(environment, SpringApplication())

        // then
        assertThat(environment.getProperty("otel.sdk.disabled")).isEqualTo("false")
        assertThat(environment.getProperty("otel.traces.exporter")).isEqualTo("otlp")
        assertThat(environment.getProperty("otel.logs.exporter")).isEqualTo("otlp")
    }

    @Test
    @DisplayName("spring.factories에 등록된 후처리기가 local 환경에서 자동으로 OTEL을 비활성화한다")
    fun springApplication_localEnvironment_disablesOtelThroughRegisteredPostProcessor() {
        val context =
            SpringApplicationBuilder(TestApplication::class.java)
                .properties("app.environment=local")
                .web(WebApplicationType.NONE)
                .run()

        try {
            assertThat(context.environment.getProperty("otel.sdk.disabled")).isEqualTo("true")
            assertThat(context.environment.getProperty("otel.traces.exporter")).isEqualTo("none")
            assertThat(context.environment.getProperty("otel.logs.exporter")).isEqualTo("none")
        } finally {
            context.close()
        }
    }

    @Configuration(proxyBeanMethods = false)
    private class TestApplication
}
