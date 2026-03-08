package com.techtaurant.mainserver.common.swagger

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeResponses(
    val value: Array<ApiErrorCodeResponse>,
)
