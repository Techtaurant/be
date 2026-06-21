package com.techtaurant.mainserver.link.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "link_crawl_failed_jobs",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_link_crawl_failed_jobs_batch_article_url", columnNames = ["batch_id", "article_url"]),
    ],
    indexes = [
        Index(name = "idx_link_crawl_failed_jobs_batch_id", columnList = "batch_id"),
        Index(name = "idx_link_crawl_failed_jobs_created_at", columnList = "created_at_utc"),
    ],
)
class LinkCrawlFailedJob(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    var batch: LinkCrawlBatch,
    @Column(name = "source_page", nullable = false)
    var sourcePage: Int,
    @Column(name = "source_page_url", nullable = false, length = 2048)
    var sourcePageUrl: String,
    @Column(name = "article_url", nullable = false, length = 2048)
    var articleUrl: String,
    @Column(length = 200)
    var title: String? = null,
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,
    @Column(name = "error_status_code", nullable = false)
    var errorStatusCode: Int,
    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    var errorMessage: String,
    @Column(name = "failure_count", nullable = false)
    var failureCount: Int = 1,
    @Column(name = "last_failed_at_utc", nullable = false)
    var lastFailedAt: Instant = Instant.now(),
) : EntityBase()
