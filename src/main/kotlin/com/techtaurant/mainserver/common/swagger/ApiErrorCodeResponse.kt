package com.techtaurant.mainserver.common.swagger

import com.techtaurant.mainserver.common.status.StatusIfs
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodeResponse(
    val statusType: KClass<out StatusIfs>,
    val values: Array<String> = [],
)
