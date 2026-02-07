package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Date
import java.util.UUID

interface PostRepository : JpaRepository<Post, UUID>, PostRepositoryCustom {
    /**
     * 게시물 상세 조회
     * author, tags, pictures, category를 JOIN FETCH하여 N+1 문제 방지
     * 모든 상태의 게시물을 조회하며, 권한 검증은 Service 레이어에서 수행
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

    /**
     * 커서 기반 DRAFT 게시물 목록 조회
     * 최근 수정일 기준 내림차순 정렬
     * 커서가 없으면 첫 페이지를 조회하고, 커서가 있으면 해당 커서 이후 데이터를 조회합니다.
     *
     * @param authorId 작성자 ID
     * @param cursorUpdatedAt 커서 updatedAt (없으면 첫 페이지)
     * @param cursorId 커서 ID (updatedAt이 같을 때 구분용)
     * @param limit 조회 개수
     * @return DRAFT 게시물 리스트
     */
    @Query(
        """
        SELECT p FROM Post p
        WHERE p.author.id = :authorId
        AND p.status = 'DRAFT'
        AND (
            p.updatedAt < :cursorUpdatedAt
            OR (p.updatedAt = :cursorUpdatedAt AND p.id < :cursorId)
        )
        ORDER BY p.updatedAt DESC, p.id DESC
    """,
    )
    fun findDraftsByAuthorWithCursor(
        @Param("authorId") authorId: UUID,
        @Param("cursorUpdatedAt") cursorUpdatedAt: Date,
        @Param("cursorId") cursorId: UUID,
        @Param("limit") limit: Int,
    ): List<Post>

    /**
     * 커서 없이 DRAFT 게시물 첫 페이지 조회
     *
     * @param authorId 작성자 ID
     * @param limit 조회 개수
     * @return DRAFT 게시물 리스트
     */
    @Query(
        """
        SELECT p FROM Post p
        WHERE p.author.id = :authorId
        AND p.status = 'DRAFT'
        ORDER BY p.updatedAt DESC, p.id DESC
    """,
    )
    fun findDraftsByAuthorFirstPage(
        @Param("authorId") authorId: UUID,
        @Param("limit") limit: Int,
    ): List<Post>

    /**
     * 게시물 조회 with author (권한 검증용)
     * 상태 필터 없이 조회하여 Service에서 권한 검증
     *
     * @param postId 게시물 ID
     * @return 게시물 엔티티 (없으면 null)
     */
    @Query(
        """
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.category
        WHERE p.id = :postId
    """,
    )
    fun findPostByIdWithAuthor(
        @Param("postId") postId: UUID,
    ): Post?

    /**
     * 게시물의 댓글 수를 원자적으로 1 증가시킵니다.
     * 락을 사용하지 않고 DB 레벨에서 안전하게 처리됩니다.
     *
     * @param postId 댓글 수를 증가시킬 게시물 ID
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    fun incrementCommentCount(
        @Param("postId") postId: UUID,
    )
}
