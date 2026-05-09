package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.post.dto.PostViewerStateResponse
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.user.application.UserBanService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PostViewerStateReadService(
    private val publishedPostReadService: PublishedPostReadService,
    private val postReadLogRepository: PostReadLogRepository,
    private val postLikeLogRepository: PostLikeLogRepository,
    private val userBanService: UserBanService,
) {
    fun getPostViewerStates(
        userId: UUID,
        postIds: List<UUID>,
    ): List<PostViewerStateResponse> {
        val posts = publishedPostReadService.getPublishedPostsByIds(postIds)
        if (posts.isEmpty()) {
            return emptyList()
        }

        val loadedPostIds = posts.mapNotNull { it.id }
        val readPostIds =
            postReadLogRepository
                .findByUserIdAndPostIdIn(userId = userId, postIds = loadedPostIds)
                .map { it.postId }
                .toSet()
        val likeStatusByPostId =
            postLikeLogRepository
                .findByUserIdAndPostIdIn(userId = userId, postIds = loadedPostIds)
                .associate { likeLog -> likeLog.post.id!! to likeLog.toLikeStatus() }
        val bannedUserIds = userBanService.getBannedUserIds(userId)

        return posts.map { post ->
            val postId = post.id!!
            PostViewerStateResponse(
                postId = postId,
                isRead = postId in readPostIds,
                likeStatus = likeStatusByPostId[postId] ?: LikeStatus.NONE,
                isBanned = post.author.id?.let { it in bannedUserIds } ?: false,
            )
        }
    }

    private fun PostLikeLog.toLikeStatus(): LikeStatus =
        if (isLiked) {
            LikeStatus.LIKE
        } else {
            LikeStatus.DISLIKE
        }
}
