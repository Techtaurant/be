package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findByUserAndPath(user: User, path: String): Category?
}
