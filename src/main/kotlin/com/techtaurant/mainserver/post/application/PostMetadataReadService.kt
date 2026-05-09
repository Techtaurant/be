package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.post.dto.PostDetailAttachmentPresignedUrlResponse
import com.techtaurant.mainserver.post.dto.PostMetadataResponse
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PostMetadataReadService(
    private val postRepository: PostRepository,
    private val attachmentService: AttachmentService,
    private val userProfileImageResolver: UserProfileImageResolver,
    @param:Value("\${app.default-post-thumbnail-url}")
    private val defaultThumbnailUrl: String,
    @param:Value("\${swagger.base-url}")
    private val baseUrl: String,
) {
    fun getPostMetadata(postIds: List<UUID>): List<PostMetadataResponse> {
        val normalizedPostIds = postIds.distinct()
        if (normalizedPostIds.isEmpty()) {
            return emptyList()
        }

        val posts = postRepository.findPublishedPostsByIdIn(normalizedPostIds)
        if (posts.isEmpty()) {
            return emptyList()
        }

        val loadedPostIds = posts.mapNotNull { it.id }
        val attachmentsByPostId =
            attachmentService.getConfirmedAttachmentsByReferenceIds(
                referenceIds = loadedPostIds,
                referenceType = AttachmentReferenceType.POST,
            )
        val presignedUrlByAttachmentId = generatePresignedUrlByAttachmentId(attachmentsByPostId)
        val authorProfileImageUrlByUserId =
            userProfileImageResolver.resolve(posts.map { it.author }.distinctBy { it.id })
        val postById = posts.associateBy { it.id!! }

        return normalizedPostIds.mapNotNull { postId ->
            postById[postId]?.let { post ->
                val attachments = attachmentsByPostId[postId].orEmpty()
                val thumbnailUrl = resolveThumbnailUrl(post, attachments, presignedUrlByAttachmentId)

                PostMetadataResponse(
                    postId = postId,
                    viewCount = post.viewCount,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    status = post.status,
                    thumbnailUrl = thumbnailUrl,
                    authorProfileImageUrl = authorProfileImageUrlByUserId[post.author.id] ?: post.author.getFallbackProfileImageUrl(),
                    attachmentPresignedUrls = buildAttachmentPresignedUrls(attachments, presignedUrlByAttachmentId),
                )
            }
        }
    }

    private fun generatePresignedUrlByAttachmentId(attachmentsByPostId: Map<UUID, List<Attachment>>): Map<UUID, String> {
        val attachments = attachmentsByPostId.values.flatten()
        if (attachments.isEmpty()) {
            return emptyMap()
        }

        return attachmentService.generatePresignedDownloadUrlMapByAttachments(attachments)
    }

    private fun resolveThumbnailUrl(
        post: Post,
        attachments: List<Attachment>,
        presignedUrlByAttachmentId: Map<UUID, String>,
    ): String {
        val thumbnailAttachment =
            post.thumbnailImage?.let { thumbnailAttachmentId ->
                attachments.firstOrNull { it.id == thumbnailAttachmentId }
            } ?: attachments.minByOrNull { it.createdAt }

        return thumbnailAttachment?.id?.let { presignedUrlByAttachmentId[it] } ?: "$baseUrl$defaultThumbnailUrl"
    }

    private fun buildAttachmentPresignedUrls(
        attachments: List<Attachment>,
        presignedUrlByAttachmentId: Map<UUID, String>,
    ): List<PostDetailAttachmentPresignedUrlResponse> {
        return attachments.mapNotNull { attachment ->
            val attachmentId = attachment.id ?: return@mapNotNull null
            presignedUrlByAttachmentId[attachmentId]?.let { presignedUrl ->
                PostDetailAttachmentPresignedUrlResponse.from(attachmentId, presignedUrl)
            }
        }
    }
}
