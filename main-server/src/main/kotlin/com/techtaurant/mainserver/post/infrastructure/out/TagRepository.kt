package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface TagRepository : JpaRepository<Tag, UUID> {
    fun findByNameIn(names: List<String>): List<Tag>

    @Query(
        value = """
            SELECT t.id, t.name, t.created_at as createdAt, t.updated_at as updatedAt,
                   COALESCE(COUNT(pt.post_id), 0) as postCount
            FROM tags t
            LEFT JOIN post_tags pt ON t.id = pt.tag_id
            WHERE (:name IS NULL OR t.name ILIKE '%' || :name || '%')
            GROUP BY t.id, t.name, t.created_at, t.updated_at
            ORDER BY postCount DESC, t.id ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findAllWithPostCount(
        @Param("name") name: String?,
        @Param("limit") limit: Int,
    ): List<TagWithPostCountProjection>

    /**
     * 커서 기반 페이지네이션으로 태그 목록 조회
     *
     * 정렬: postCount DESC, id ASC (게시물 많은 순, 동점이면 id 순)
     * 커서: 마지막으로 조회한 태그의 (postCount, id) 조합
     *
     * 다음 페이지 조건:
     * - postCount가 커서보다 작거나
     * - postCount가 같으면 id가 커서보다 큰 항목
     */
    @Query(
        value = """
            SELECT t.id, t.name, t.created_at as createdAt, t.updated_at as updatedAt,
                   COALESCE(COUNT(pt.post_id), 0) as postCount
            FROM tags t
            LEFT JOIN post_tags pt ON t.id = pt.tag_id
            WHERE (:name IS NULL OR t.name ILIKE '%' || :name || '%')
            GROUP BY t.id, t.name, t.created_at, t.updated_at
            HAVING COALESCE(COUNT(pt.post_id), 0) < :lastPostCount
                OR (COALESCE(COUNT(pt.post_id), 0) = :lastPostCount AND t.id > :lastTagId)
            ORDER BY postCount DESC, t.id ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findAllWithPostCountAfterCursor(
        @Param("name") name: String?,
        @Param("lastPostCount") lastPostCount: Long,
        @Param("lastTagId") lastTagId: UUID,
        @Param("limit") limit: Int,
    ): List<TagWithPostCountProjection>
}
