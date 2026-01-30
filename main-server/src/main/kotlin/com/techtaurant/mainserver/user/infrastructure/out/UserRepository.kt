package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByIdentifierAndProvider(identifier: String, provider: OAuthProvider): User?

    @Query(
        """
        SELECT u.* FROM users u
        WHERE
           -- pg_trgm을 활용한 부분 문자열 매칭
           -- 대소문자 구분 없이 검색어가 포함된 모든 사용자 찾기
           u.name ILIKE '%' || :name || '%'
        ORDER BY
           -- Trigram 유사도로 정렬 (검색어와의 일치도)
           similarity(u.name, :name) DESC,
           u.name ASC
        """,
        nativeQuery = true
    )
    fun findByNameContainingIgnoreCaseOrderByNameAsc(@Param("name") name: String): List<User>
}
