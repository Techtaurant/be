package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findByUserAndPath(
        user: User,
        path: String,
    ): Category?

    /**
     * 유저의 전체 카테고리 조회
     */
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId ORDER BY c.depth ASC, c.name ASC")
    fun findByUserId(
        @Param("userId") userId: UUID,
    ): List<Category>

    /**
     * 유저의 특정 path prefix로 시작하는 카테고리 조회
     * Native query + LIKE 'prefix%' 패턴으로 B-tree 인덱스(idx_category_user_path_prefix) 활용
     */
    @Query(
        value = """
            SELECT c.* FROM categories c
            WHERE c.user_id = :userId
              AND c.path LIKE :pathPrefix || '%'
            ORDER BY c.depth ASC, c.name ASC
        """,
        nativeQuery = true,
    )
    fun findByUserIdAndPathPrefix(
        @Param("userId") userId: UUID,
        @Param("pathPrefix") pathPrefix: String,
    ): List<Category>
}
