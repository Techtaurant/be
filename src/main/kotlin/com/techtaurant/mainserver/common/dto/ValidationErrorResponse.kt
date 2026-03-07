package com.techtaurant.mainserver.common.dto

data class ValidationErrorResponse(
    val errors: Map<String, String>,
)
