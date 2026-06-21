package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.link.application.LinkBatchRunService
import com.techtaurant.mainserver.link.application.LinkCrawlBatchAdminService
import com.techtaurant.mainserver.link.dto.CreateLinkCrawlBatchRequest
import com.techtaurant.mainserver.link.dto.LinkBatchRunResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlBatchListItemResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlBatchResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlFailedJobResponse
import com.techtaurant.mainserver.link.dto.UpdateLinkCrawlBatchRequest
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminLinkCrawlBatchController(
    private val linkCrawlBatchAdminService: LinkCrawlBatchAdminService,
    private val linkBatchRunService: LinkBatchRunService,
) : AdminLinkCrawlBatchControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PostMapping("${SecurityConstants.ADMIN_API_PREFIX}/companies/{companyUserId}/link-crawl-batches")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createBatch(
        @PathVariable companyUserId: UUID,
        @Valid @RequestBody request: CreateLinkCrawlBatchRequest,
    ): ApiResponse<LinkCrawlBatchResponse> {
        return ApiResponse.created(linkCrawlBatchAdminService.createBatch(companyUserId, request))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @GetMapping("${SecurityConstants.ADMIN_API_PREFIX}/companies/{companyUserId}/link-crawl-batches")
    override fun getBatches(
        @PathVariable companyUserId: UUID,
    ): ApiResponse<List<LinkCrawlBatchListItemResponse>> {
        return ApiResponse.ok(linkCrawlBatchAdminService.getBatches(companyUserId))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PatchMapping("${SecurityConstants.ADMIN_API_PREFIX}/link-crawl-batches/{batchId}")
    override fun updateBatch(
        @PathVariable batchId: UUID,
        @Valid @RequestBody request: UpdateLinkCrawlBatchRequest,
    ): ApiResponse<LinkCrawlBatchResponse> {
        return ApiResponse.ok(linkCrawlBatchAdminService.updateBatch(batchId, request))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @PostMapping("${SecurityConstants.ADMIN_API_PREFIX}/link-crawl-batches/{batchId}/run")
    override fun runBatch(
        @PathVariable batchId: UUID,
    ): ApiResponse<LinkBatchRunResponse> {
        return ApiResponse.ok(linkBatchRunService.run(batchId))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @GetMapping("${SecurityConstants.ADMIN_API_PREFIX}/link-crawl-batches/{batchId}/failed-jobs")
    override fun getFailedJobs(
        @PathVariable batchId: UUID,
    ): ApiResponse<List<LinkCrawlFailedJobResponse>> {
        return ApiResponse.ok(linkBatchRunService.getFailedJobs(batchId))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @PostMapping("${SecurityConstants.ADMIN_API_PREFIX}/link-crawl-failed-jobs/{failedJobId}/run")
    override fun runFailedJob(
        @PathVariable failedJobId: UUID,
    ): ApiResponse<LinkBatchRunResponse> {
        return ApiResponse.ok(linkBatchRunService.runFailedJob(failedJobId))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @DeleteMapping("${SecurityConstants.ADMIN_API_PREFIX}/link-crawl-failed-jobs/{failedJobId}")
    override fun deleteFailedJob(
        @PathVariable failedJobId: UUID,
    ): ApiResponse<Unit> {
        linkBatchRunService.deleteFailedJob(failedJobId)
        return ApiResponse.ok()
    }
}
