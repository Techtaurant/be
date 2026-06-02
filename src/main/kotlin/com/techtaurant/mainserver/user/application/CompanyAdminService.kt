package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.comment.application.CommentDeleteService
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.application.LinkDailyStatsService
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkLikeLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.application.PostDailyStatsService
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.dto.CompanyResponse
import com.techtaurant.mainserver.user.dto.CreateCompanyRequest
import com.techtaurant.mainserver.user.dto.CreateUserTokenRequest
import com.techtaurant.mainserver.user.dto.UserTokenResponse
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserToken
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserTokenRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CompanyAdminService(
    private val userRepository: UserRepository,
    private val userTokenRepository: UserTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val attachmentService: AttachmentService,
    private val userResponseAssembler: UserResponseAssembler,
    private val linkCrawlBatchRepository: LinkCrawlBatchRepository,
    private val linkRepository: LinkRepository,
    private val postRepository: PostRepository,
    private val userLinkRepository: UserLinkRepository,
    private val linkLikeLogRepository: LinkLikeLogRepository,
    private val linkDailyStatsService: LinkDailyStatsService,
    private val commentRepository: CommentRepository,
    private val commentLikeLogRepository: CommentLikeLogRepository,
    private val commentDeleteService: CommentDeleteService,
    private val postLikeLogRepository: PostLikeLogRepository,
    private val postDailyStatsService: PostDailyStatsService,
) {
    companion object {
        private const val USER_NAME_UNIQUE_CONSTRAINT = "uk_users_name"
    }

    @Transactional
    fun createCompany(request: CreateCompanyRequest): CompanyResponse {
        val normalizedName = request.name.trim()
        val normalizedEmail = request.email.trim()

        if (normalizedName.isEmpty()) {
            throw ApiException(DefaultStatus.BAD_REQUEST, "회사 이름은 공백일 수 없습니다")
        }

        if (normalizedEmail.isEmpty()) {
            throw ApiException(DefaultStatus.BAD_REQUEST, "회사 이메일은 공백일 수 없습니다")
        }

        val attachmentId = request.parseServiceProfileImageAttachmentId()
        val company =
            User(
                name = normalizedName,
                email = normalizedEmail,
                provider = OAuthProvider.SYSTEM,
                identifier = "company:${UUID.randomUUID()}",
                role = UserRole.COMPANY,
                profileImageUrl = request.profileImageUrl?.trim().orEmpty(),
                serviceProfileImageAttachmentId = attachmentId,
            )

        val savedCompany =
            try {
                userRepository.saveAndFlush(company)
            } catch (exception: DataIntegrityViolationException) {
                if (isUserNameUniqueConstraintViolation(exception)) {
                    throw ApiException(UserStatus.USER_NAME_ALREADY_EXISTS)
                }
                throw exception
            }

        if (attachmentId != null) {
            attachmentService.confirmAttachmentsByIds(
                referenceId = savedCompany.id ?: throw ApiException(UserStatus.ID_NOT_FOUND),
                referenceType = AttachmentReferenceType.USER,
                attachmentIds = listOf(attachmentId),
            )
        }

        return CompanyResponse.from(userResponseAssembler.assemble(savedCompany))
    }

    @Transactional(readOnly = true)
    fun getCompanies(): List<CompanyResponse> {
        return userRepository.findAllByRoleOrderByNameAsc(UserRole.COMPANY)
            .map(userResponseAssembler::assemble)
            .map(CompanyResponse::from)
    }

    @Transactional
    fun deleteCompany(companyUserId: UUID) {
        val companyUser = getCompanyUser(companyUserId)
        val companyId = companyUser.id ?: throw ApiException(UserStatus.ID_NOT_FOUND)

        linkCrawlBatchRepository.deleteAllByCompanyUserId(companyId)
        linkRepository.deleteAllOnlyConnectedByCompanyUserId(companyId)
        decrementSavedLinkStats(companyId)
        adjustLinkLikeStats(companyId)
        adjustPostLikeStats(companyId)
        adjustCommentLikeStats(companyId)
        softDeleteAuthoredCommentsOnSurvivingPosts(companyId)
        postRepository.findIdsByAuthorId(companyId).forEach { postId ->
            attachmentService.deleteAttachmentsByReference(postId, AttachmentReferenceType.POST)
        }
        attachmentService.deleteAttachmentsByReference(companyId, AttachmentReferenceType.USER)
        userRepository.delete(companyUser)
    }

    @Transactional
    fun createCompanyToken(
        companyUserId: UUID,
        request: CreateUserTokenRequest,
    ): UserTokenResponse {
        val companyUser = getCompanyUser(companyUserId)
        val normalizedName = request.name.trim()

        if (normalizedName.isEmpty()) {
            throw ApiException(DefaultStatus.BAD_REQUEST, "토큰 이름은 공백일 수 없습니다")
        }

        val companyUserIdValue = companyUser.id ?: throw ApiException(UserStatus.ID_NOT_FOUND)
        val token = jwtTokenProvider.createPermanentAccessToken(companyUserIdValue, companyUser.role)

        userTokenRepository.deleteAllByUserId(companyUserIdValue)
        userTokenRepository.flush()

        val userToken =
            userTokenRepository.saveAndFlush(
                UserToken(
                    user = companyUser,
                    name = normalizedName,
                    tokenHash = jwtTokenProvider.hashToken(token),
                ),
            )

        return UserTokenResponse.from(userToken, token)
    }

    private fun decrementSavedLinkStats(companyId: UUID) {
        userLinkRepository.findSavedByUserId(companyId).forEach { savedLink ->
            linkDailyStatsService.decrementSaveCount(
                linkId = savedLink.link.id ?: return@forEach,
                statDate = DateUtils.toUtcDate(savedLink.createdAt),
            )
        }
    }

    private fun adjustLinkLikeStats(companyId: UUID) {
        linkLikeLogRepository.findAllByUserIdWithLink(companyId).forEach { likeLog ->
            val linkId = likeLog.link.id ?: return@forEach
            val statDate = DateUtils.toUtcDate(likeLog.createdAt)
            if (likeLog.isLiked) {
                linkRepository.decrementLikeCount(linkId)
                linkDailyStatsService.decrementLikeCount(linkId, statDate)
            } else {
                linkRepository.incrementLikeCount(linkId)
                linkDailyStatsService.incrementLikeCount(linkId, statDate)
            }
        }
    }

    private fun adjustPostLikeStats(companyId: UUID) {
        postLikeLogRepository.findAllByUserIdWithSurvivingPost(companyId).forEach { likeLog ->
            val postId = likeLog.post.id ?: return@forEach
            val statDate = DateUtils.toUtcDate(likeLog.createdAt)
            if (likeLog.isLiked) {
                postRepository.decrementLikeCount(postId)
                postDailyStatsService.decrementLikeCount(postId, statDate)
            } else {
                postRepository.incrementLikeCount(postId)
                postDailyStatsService.incrementLikeCount(postId, statDate)
            }
        }
    }

    private fun adjustCommentLikeStats(companyId: UUID) {
        commentLikeLogRepository.findAllByUserIdWithCommentsOnSurvivingPosts(companyId).forEach { likeLog ->
            val commentId = likeLog.comment.id ?: return@forEach
            if (likeLog.isLiked) {
                commentRepository.decrementLikeCount(commentId)
            } else {
                commentRepository.incrementLikeCount(commentId)
            }
        }
    }

    private fun softDeleteAuthoredCommentsOnSurvivingPosts(companyId: UUID) {
        commentRepository.findActiveIdsByAuthorIdAndPostAuthorIdNot(companyId)
            .forEach { commentId -> commentDeleteService.deleteComment(commentId, companyId) }
    }

    private fun isUserNameUniqueConstraintViolation(exception: DataIntegrityViolationException): Boolean {
        return generateSequence<Throwable>(exception) { current -> current.cause }
            .mapNotNull { throwable -> throwable.message }
            .any { message -> message.contains(USER_NAME_UNIQUE_CONSTRAINT, ignoreCase = true) }
    }

    private fun getCompanyUser(companyUserId: UUID): User {
        val user =
            userRepository.findById(companyUserId).orElseThrow {
                ApiException(UserStatus.COMPANY_NOT_FOUND)
            }

        if (user.role != UserRole.COMPANY) {
            throw ApiException(UserStatus.COMPANY_NOT_FOUND)
        }

        return user
    }
}
