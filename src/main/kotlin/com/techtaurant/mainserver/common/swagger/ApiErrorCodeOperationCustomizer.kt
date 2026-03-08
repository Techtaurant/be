package com.techtaurant.mainserver.common.swagger

import com.fasterxml.jackson.databind.ObjectMapper
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.ValidationErrorResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.status.StatusIfs
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import io.swagger.v3.oas.models.examples.Example as SwaggerExample
import io.swagger.v3.oas.models.responses.ApiResponse as SwaggerApiResponse

@Component
class ApiErrorCodeOperationCustomizer(
    private val objectMapper: ObjectMapper,
) : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        val annotations = collectAnnotations(handlerMethod)
        if (annotations.isEmpty()) return operation

        if (operation.responses == null) {
            operation.responses = ApiResponses()
        }

        val statusEntries = extractStatusEntries(annotations)
        val groupedByHttpStatus = statusEntries.groupBy { it.getHttpStatusCode().toString() }

        groupedByHttpStatus.forEach { (httpStatusCode, statuses) ->
            addErrorResponse(operation, httpStatusCode, statuses)
        }

        return operation
    }

    private fun collectAnnotations(handlerMethod: HandlerMethod): List<ApiErrorCodeResponse> {
        val annotations = mutableListOf<ApiErrorCodeResponse>()

        handlerMethod.getMethodAnnotation(ApiErrorCodeResponse::class.java)?.let {
            annotations.add(it)
        }

        handlerMethod.getMethodAnnotation(ApiErrorCodeResponses::class.java)?.let {
            annotations.addAll(it.value)
        }

        return annotations
    }

    private fun extractStatusEntries(annotations: List<ApiErrorCodeResponse>): List<StatusIfs> {
        return annotations.flatMap { annotation ->
            val enumClass = annotation.statusType.java
            val allConstants = enumClass.enumConstants?.filterIsInstance<StatusIfs>() ?: emptyList()
            val requestedValues = annotation.values

            if (requestedValues.isEmpty()) {
                allConstants
            } else {
                val valueNames = requestedValues.toSet()
                allConstants.filter { (it as Enum<*>).name in valueNames }
            }
        }
    }

    private fun addErrorResponse(
        operation: Operation,
        httpStatusCode: String,
        statuses: List<StatusIfs>,
    ) {
        val existing = operation.responses?.get(httpStatusCode)
        val response = existing ?: SwaggerApiResponse()

        if (response.description.isNullOrBlank()) {
            response.description = resolveDescription(httpStatusCode)
        }

        val content = response.content ?: Content()
        val mediaType = content["application/json"] ?: MediaType()

        if (mediaType.schema == null) {
            mediaType.schema =
                Schema<Any>().apply {
                    `$ref` = "#/components/schemas/ApiResponse"
                }
        }

        statuses.forEach { status ->
            val exampleJson = buildErrorJson(status)
            val example =
                SwaggerExample().apply {
                    value = exampleJson
                    description = status.getDescription()
                }
            mediaType.addExamples(resolveExampleName(status), example)
        }

        if (httpStatusCode == "400") {
            addValidationErrorExample(mediaType)
        }

        content.addMediaType("application/json", mediaType)
        response.content = content
        operation.responses.addApiResponse(httpStatusCode, response)
    }

    private fun buildErrorJson(status: StatusIfs): String {
        val errorResponse = ApiResponse.error<Any>(status)
        return objectMapper.writeValueAsString(errorResponse)
    }

    private fun addValidationErrorExample(mediaType: MediaType) {
        val validationData = ValidationErrorResponse(mapOf("field" to "에러 메시지"))
        val validationResponse = ApiResponse.error(DefaultStatus.BAD_REQUEST, validationData)
        val example =
            SwaggerExample().apply {
                value = objectMapper.writeValueAsString(validationResponse)
                description = DefaultStatus.BAD_REQUEST.getDescription()
            }
        mediaType.addExamples("Validation 에러", example)
    }

    private fun resolveDescription(httpStatusCode: String): String =
        when (httpStatusCode) {
            "400" -> "입력값 검증 실패 또는 비즈니스 규칙 위반"
            "401" -> "인증 필요"
            "403" -> "권한 없음"
            "404" -> "리소스 미존재"
            "409" -> "동시 요청 충돌"
            "500" -> "서버 오류"
            else -> "에러"
        }

    private fun resolveExampleName(status: StatusIfs): String = status.getDescription()
}
