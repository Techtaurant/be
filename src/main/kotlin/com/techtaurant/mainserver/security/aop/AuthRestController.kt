package com.techtaurant.mainserver.security.aop

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RestController

@RestController
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag(name = "Auth", description = "인증 API")
annotation class AuthRestController
