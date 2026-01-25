package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*

/**
 * 게시물 엔티티
 *
 * @property title 게시물 제목 (최대 200자)
 * @property content 게시물 본문 (TEXT 타입, 제한 없음)
 * @property contentTsVector PostgreSQL tsvector 컬럼으로 한국어 전문 검색 지원
 * @property author 작성자 (User와 N:1 관계)
 * @property category 카테고리 (Category와 N:1 관계)
 * @property tags 태그 목록 (ManyToMany)
 */
@Entity
@Table(name = "posts")
class Post(

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "post_tags",
        joinColumns = [JoinColumn(name = "post_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    var tags: MutableSet<Tag> = mutableSetOf(),

) : EntityBase()
