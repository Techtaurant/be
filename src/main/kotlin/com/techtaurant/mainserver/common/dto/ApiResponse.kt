package com.techtaurant.mainserver.common.dto

import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.status.StatusIfs
import org.springframework.http.HttpStatus

class ApiResponse<T>(val status: Int, val data: T?, val message: String) {
    companion object {
        fun <T> ok(): ApiResponse<T> {
            return ApiResponse(HttpStatus.OK.value(), null, DefaultStatus.OK.getDescription())
        }

        fun <T> ok(data: T): ApiResponse<T> {
            return ApiResponse(HttpStatus.OK.value(), data, DefaultStatus.OK.getDescription())
        }

        fun <T> error(status: StatusIfs): ApiResponse<T?> {
            return ApiResponse(status.getCustomStatusCode(), null, status.getDescription())
        }

        fun <T> error(
            status: StatusIfs,
            data: T?,
        ): ApiResponse<T> {
            return ApiResponse(status.getCustomStatusCode(), data, status.getDescription())
        }
    }
}
