package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.post.enums.TagTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 태그 엔티티
 *
 * @property name 태그 이름
 * @property targetType 태그 대상 타입
 */
@Entity
@Table(
    name = "tags",
    uniqueConstraints = [UniqueConstraint(name = "uk_tags_name_target_type", columnNames = ["name", "target_type"])],
)
class Tag(
    @Column(nullable = false, length = 50)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    var targetType: TagTargetType = TagTargetType.POST,
) : EntityBase()
