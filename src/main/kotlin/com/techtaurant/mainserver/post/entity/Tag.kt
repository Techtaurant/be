package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 태그 엔티티
 *
 * @property name 태그 이름
 */
@Entity
@Table(
    name = "tags",
    uniqueConstraints = [UniqueConstraint(name = "uk_tags_name", columnNames = ["name"])],
)
class Tag(
    @Column(nullable = false, length = 50)
    var name: String,
) : EntityBase()
