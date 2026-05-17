package com.techtaurant.mainserver.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class ProductionToolingDisableEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val appEnvironment = environment.getProperty(APP_ENVIRONMENT_PROPERTY)?.trim()
        if (!appEnvironment.equals(PROD_ENVIRONMENT, ignoreCase = true)) {
            return
        }

        environment.propertySources.addFirst(
            MapPropertySource(
                PROPERTY_SOURCE_NAME,
                mapOf(
                    SPRINGDOC_API_DOCS_ENABLED_PROPERTY to FALSE_VALUE,
                    SPRINGDOC_SWAGGER_UI_ENABLED_PROPERTY to FALSE_VALUE,
                    P6SPY_ENABLE_LOGGING_PROPERTY to FALSE_VALUE,
                ),
            ),
        )
    }

    companion object {
        private const val PROPERTY_SOURCE_NAME = "productionToolingDisableOverrides"
        private const val APP_ENVIRONMENT_PROPERTY = "app.environment"
        private const val PROD_ENVIRONMENT = "prod"
        private const val SPRINGDOC_API_DOCS_ENABLED_PROPERTY = "springdoc.api-docs.enabled"
        private const val SPRINGDOC_SWAGGER_UI_ENABLED_PROPERTY = "springdoc.swagger-ui.enabled"
        private const val P6SPY_ENABLE_LOGGING_PROPERTY = "decorator.datasource.p6spy.enable-logging"
        private const val FALSE_VALUE = "false"
    }
}
