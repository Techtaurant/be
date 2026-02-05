package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PostRepository : JpaRepository<Post, UUID>, PostRepositoryCustom {
    /**
     * 게시물 상세 조회
     * author, tags, pictures, category를 JOIN FETCH하여 N+1 문제 방지
     *
     * @param postId 게시물 ID
     * @return 게시물 엔티티 (없으면 null)
     */
    @Query(
        """
        SELECT p FROM Post p
        JOIN FETCH p.author
        LEFT JOIN FETCH p.tags
        LEFT JOIN FETCH p.pictures
        LEFT JOIN FETCH p.category
        WHERE p.id = :postId
    """,
    )
    fun findPostDetailById(postId: UUID): Post?
}
