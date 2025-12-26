package com.techtaurant.mainserver.articlelink.infrastructure.out

import com.techtaurant.mainserver.articlelink.entity.ArticleLink
import com.techtaurant.mainserver.articlelink.enums.ArticleLinkType
import com.techtaurant.mainserver.blog.entity.Blog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ArticleLinkRepository : JpaRepository<ArticleLink, UUID> {
    fun findByBlogAndDeletedAtIsNull(blog: Blog): List<ArticleLink>
    fun findByTypeAndDeletedAtIsNull(type: ArticleLinkType): List<ArticleLink>
}
