package com.techtaurant.mainserver.blog

import com.techtaurant.mainserver.common.status.StatusIfs

enum class BlogStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    BLOG_NOT_FOUND(404, 3001, "Blog not found"), ;

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
