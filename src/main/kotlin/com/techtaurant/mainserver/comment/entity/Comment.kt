package com.techtaurant.mainserver.comment.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*
import java.util.Date

/**
 * 댓글 엔티티
 * 게시물에 대한 댓글을 표현하며, 댓글에 대한 대댓글까지 지원합니다(최대 1depth).
 * 삭제 시 실제 DELETE 대신 삭제 시각을 기록하고, 내용은 서비스 레이어에서 해시 문자열로 치환합니다.
 *
 * @property content 댓글 내용
 * @property post 댓글이 달린 게시물
 * @property author 댓글 작성자
 * @property parent 부모 댓글 (대댓글인 경우만 값이 있음)
 * @property depth 댓글 깊이 (0: 일반 댓글, 1: 대댓글)
 * @property likeCount 좋아요 수
 * @property replyCount 대댓글 수 (depth=0인 댓글에만 의미 있음)
 * @property children 자식 댓글 (대댓글들)
 * @property deletedAt 삭제 시각
 */
@Entity
@Table(name = "comments")
class Comment(
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    var author: User?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Comment? = null,
    @Column(nullable = false)
    var depth: Int = 0,
    @Column(nullable = false)
    var likeCount: Long = 0,
    @Column(nullable = false)
    var replyCount: Long = 0,
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    var children: MutableList<Comment> = mutableListOf(),
    @Column(name = "deleted_at")
    var deletedAt: Date? = null,
) : EntityBase()
