package com.techtaurant.mainserver.link.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.entity.TaggedContent
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "links",
    uniqueConstraints = [UniqueConstraint(name = "uk_links_url", columnNames = ["url"])],
)
class Link(
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(nullable = false, length = 2048)
    var url: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String,
    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "link_tags",
        joinColumns = [JoinColumn(name = "link_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    override var tags: MutableSet<Tag> = mutableSetOf(),
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    createdAt: Instant = Instant.now(),
) : EntityBase(createdAt = createdAt), TaggedContent {
    init {
        validateTagCount()
    }
}
