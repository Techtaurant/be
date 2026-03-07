package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostLikeLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PostLikeLogRepository : JpaRepository<PostLikeLog, UUID> {
    /**
     * 특정 게시글에 대한 특정 사용자의 좋아요 로그를 조회합니다.
     * 기존 로그가 있는지 확인하여 UPDATE 또는 INSERT를 결정하는 데 사용됩니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 로그 (없으면 null)
     */
    fun findByPostIdAndUserId(
        postId: UUID,
        userId: UUID,
    ): PostLikeLog?
}
