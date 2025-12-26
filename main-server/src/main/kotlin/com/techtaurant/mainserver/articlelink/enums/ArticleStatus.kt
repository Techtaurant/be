package com.techtaurant.mainserver.articlelink.enums

import com.techtaurant.mainserver.common.status.StatusIfs

enum class ArticleStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    ARTICLE_NOT_FOUND(404, 2001, "Article not found"), ;

    override fun getHttpStatusCode(): Int {
        return this.httpStatusCode
    }

    override fun getCustomStatusCode(): Int {
        return this.customStatusCode
    }

    override fun getDescription(): String {
        return this.description
    }
}
