package com.techtaurant.mainserver.blog.infrastructure.out

import com.techtaurant.mainserver.blog.entity.Blog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BlogRepository : JpaRepository<Blog, UUID> {
    fun findByNameAndDeletedAtIsNull(name: String): Blog?
    fun findByDeletedAtIsNull(): List<Blog>

    /**
     * ILIKE를 사용한 부분 문자열 검색
     * name 또는 displayName에 keyword가 포함된 블로그를 검색
     */
    @Query(
        """
        SELECT b FROM Blog b
        WHERE (LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND b.deletedAt IS NULL
        """
    )
    fun searchByKeyword(@Param("keyword") keyword: String): List<Blog>

    /**
     * Trigram 유사도 검색 (Full Text Index 활용)
     * name + displayName 복합 검색으로 유사도 순 정렬
     * pg_trgm extension과 GIN 인덱스 활용
     */
    @Query(
        value = """
        SELECT * FROM blogs
        WHERE (name || ' ' || COALESCE(display_name, '')) % :keyword
          AND deleted_at IS NULL
        ORDER BY similarity((name || ' ' || COALESCE(display_name, '')), :keyword) DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchByTrigramSimilarity(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int = 10
    ): List<Blog>
}
