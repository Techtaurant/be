package com.techtaurant.mainserver.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class LocalOtelDisableEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val appEnvironment = environment.getProperty(APP_ENVIRONMENT_PROPERTY)?.trim()
        if (!appEnvironment.equals(LOCAL_ENVIRONMENT, ignoreCase = true)) {
            return
        }

        environment.propertySources.addFirst(
            MapPropertySource(
                PROPERTY_SOURCE_NAME,
                mapOf(
                    OTEL_SDK_DISABLED_PROPERTY to TRUE_VALUE,
                    OTEL_TRACES_EXPORTER_PROPERTY to NONE_VALUE,
                    OTEL_LOGS_EXPORTER_PROPERTY to NONE_VALUE,
                ),
            ),
        )
    }

    companion object {
        private const val PROPERTY_SOURCE_NAME = "localOtelDisableOverrides"
        private const val APP_ENVIRONMENT_PROPERTY = "app.environment"
        private const val LOCAL_ENVIRONMENT = "local"
        private const val OTEL_SDK_DISABLED_PROPERTY = "otel.sdk.disabled"
        private const val OTEL_TRACES_EXPORTER_PROPERTY = "otel.traces.exporter"
        private const val OTEL_LOGS_EXPORTER_PROPERTY = "otel.logs.exporter"
        private const val TRUE_VALUE = "true"
        private const val NONE_VALUE = "none"
    }
}
