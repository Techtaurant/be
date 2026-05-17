package com.techtaurant.mainserver.common.swagger

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeResponses(
    val value: Array<ApiErrorCodeResponse>,
)
