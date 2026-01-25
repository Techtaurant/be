package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.*

/**
 * 계층형 카테고리 엔티티
 * 최대 5단계 depth까지 지원하며, 경로 기반으로 카테고리를 관리합니다.
 * 각 유저별로 동일한 path를 가질 수 없습니다.
 *
 * @property user 카테고리 소유자
 * @property name 카테고리 이름 (예: "spring")
 * @property path 전체 경로 (예: "java/spring/deepdive")
 * @property depth 현재 depth (1~5)
 * @property parent 부모 카테고리 (nullable)
 */
@Entity
@Table(
    name = "categories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "path"])],
    indexes = [
        Index(name = "idx_category_user_depth", columnList = "user_id, depth"),
        Index(name = "idx_category_user_name", columnList = "user_id, name"),
        Index(name = "idx_category_user_path_prefix", columnList = "user_id, path")
    ]
)
class Category(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 500)
    var path: String,

    @Column(nullable = false)
    var depth: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    var children: MutableList<Category> = mutableListOf(),

) : EntityBase()
