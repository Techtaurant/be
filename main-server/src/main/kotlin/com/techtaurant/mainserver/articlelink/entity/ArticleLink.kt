package com.techtaurant.mainserver.articlelink.entity

import com.techtaurant.mainserver.articlelink.enums.ArticleLinkType
import com.techtaurant.mainserver.blog.entity.Blog
import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "article_links")
class ArticleLink(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    var blog: Blog,

    @Column(nullable = false, columnDefinition = "TEXT")
    var url: String,

    @Column(length = 500)
    var title: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: ArticleLinkType,

    @Column(name = "deleted_at")
    var deletedAt: Date? = null,
) : EntityBase()
