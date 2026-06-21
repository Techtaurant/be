package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.CompanyAdminService
import com.techtaurant.mainserver.user.dto.CompanyResponse
import com.techtaurant.mainserver.user.dto.CreateCompanyRequest
import com.techtaurant.mainserver.user.dto.CreateUserTokenRequest
import com.techtaurant.mainserver.user.dto.UserTokenResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.ADMIN_API_PREFIX}/companies")
class AdminCompanyController(
    private val companyAdminService: CompanyAdminService,
) : AdminCompanyControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCompany(
        @Valid @RequestBody request: CreateCompanyRequest,
    ): ApiResponse<CompanyResponse> {
        return ApiResponse.created(companyAdminService.createCompany(request))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @GetMapping
    override fun getCompanies(): ApiResponse<List<CompanyResponse>> {
        return ApiResponse.ok(companyAdminService.getCompanies())
    }

    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PostMapping("/{companyUserId}/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCompanyToken(
        @PathVariable companyUserId: UUID,
        @Valid @RequestBody request: CreateUserTokenRequest,
    ): ApiResponse<UserTokenResponse> {
        return ApiResponse.created(companyAdminService.createCompanyToken(companyUserId, request))
    }

    @ApiErrorResponses(includeAuthenticationErrors = true)
    @DeleteMapping("/{companyUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteCompany(
        @PathVariable companyUserId: UUID,
    ) {
        companyAdminService.deleteCompany(companyUserId)
    }
}
