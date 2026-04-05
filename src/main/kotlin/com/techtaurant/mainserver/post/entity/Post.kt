package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 게시물 엔티티
 *
 * @property title 게시물 제목 (최대 200자)
 * @property content 게시물 본문 (TEXT 타입, 제한 없음)
 * @property author 작성자 (User와 N:1 관계)
 * @property category 카테고리 (Category와 N:1 관계)
 * @property tags 태그 목록 (ManyToMany)
 * @property viewCount 조회수 (이벤트 발생 시 원자적 증분)
 * @property likeCount 좋아요수 (이벤트 발생 시 원자적 증분)
 * @property commentCount 댓글수 (이벤트 발생 시 원자적 증분)
 * @property thumbnailImage 게시물 썸네일 이미지 attachment ID
 * @property status 게시물 상태 (DRAFT: 임시저장, PUBLISHED: 발행, PRIVATE: 비공개)
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
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    var tags: MutableSet<Tag> = mutableSetOf(),
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0,
    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0,
    @Column(name = "thumbnail_image")
    var thumbnailImage: java.util.UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PostStatusEnum = PostStatusEnum.PUBLISHED,
) : EntityBase()
