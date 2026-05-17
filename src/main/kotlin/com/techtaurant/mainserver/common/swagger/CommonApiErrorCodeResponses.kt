package com.techtaurant.mainserver.common.swagger

import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiErrorCodeResponses(
    [
        ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
    ],
)
annotation class ApiCommonBadRequestAndUnknown

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiErrorCodeResponses(
    [
        ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
    ],
)
annotation class ApiCommonBadRequestUnknownAndAuthenticationRequired
