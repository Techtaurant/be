package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.*

/**
 * 태그 엔티티
 *
 * @property name 태그 이름 (unique)
 */
@Entity
@Table(
    name = "tags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])],
)
class Tag(
    @Column(nullable = false, length = 50, unique = true)
    var name: String,
) : EntityBase()
