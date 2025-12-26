package com.techtaurant.mainserver.articlelink.service

import com.techtaurant.mainserver.articlelink.dto.ValidationRequest
import com.techtaurant.mainserver.articlelink.entity.ArticleLink
import com.techtaurant.mainserver.articlelink.infrastructure.out.ArticleLinkRepository
import com.techtaurant.mainserver.blog.BlogStatus
import com.techtaurant.mainserver.blog.entity.Blog
import com.techtaurant.mainserver.blog.infrastructure.out.BlogRepository
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.infrastructure.out.crawler.CrawlerApiClient
import com.techtaurant.mainserver.infrastructure.out.crawler.dto.PageBaseLinkValidationRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArticleLinkService(
    private val articleLinkRepository: ArticleLinkRepository,
    private val blogRepository: BlogRepository,
    private val crawlerApiClient: CrawlerApiClient,
) {

    @Transactional
    fun validateAndSave(request: ValidationRequest): Boolean {
        // Blog 조회
        val blog = blogRepository.findById(request.blogId)
            .orElseThrow { ApiException(BlogStatus.BLOG_NOT_FOUND) }

        val crawlerRequest = PageBaseLinkValidationRequest(
            blogName = blog.name,
            baseUrl = request.baseUrl,
            startPage = request.startPage,
            articlePattern = request.articlePattern,
            titleSelector = request.titleSelector,
        )

        val isValid = crawlerApiClient.validateCrawlingPage(crawlerRequest)

        if (isValid) {
            val articleLink = ArticleLink(
                blog = blog,
                url = request.baseUrl,
                title = null,
                type = request.type,
            )
            articleLinkRepository.save(articleLink)
        }

        return isValid
    }
}
