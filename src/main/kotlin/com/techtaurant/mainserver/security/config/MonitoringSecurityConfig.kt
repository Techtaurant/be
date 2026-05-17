package com.techtaurant.mainserver.security.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
class MonitoringSecurityConfig(
    @Value("\${monitoring.metrics.username:prometheus}")
    private val metricsUsername: String,
    @Value("\${monitoring.metrics.password:change-me-prometheus-password}")
    private val metricsPassword: String,
    private val transactionIdMdcFilter: TransactionIdMdcFilter,
) {
    @Bean
    @Order(0)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher(EndpointRequest.to("health", "prometheus"))
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(EndpointRequest.to("health")).permitAll()
                    .requestMatchers(EndpointRequest.to("prometheus")).hasRole("PROMETHEUS")
            }
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(transactionIdMdcFilter, BasicAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun metricsUserDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val metricsUser =
            User
                .withUsername(metricsUsername)
                .password(passwordEncoder.encode(metricsPassword))
                .roles("PROMETHEUS")
                .build()

        return InMemoryUserDetailsManager(metricsUser)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
