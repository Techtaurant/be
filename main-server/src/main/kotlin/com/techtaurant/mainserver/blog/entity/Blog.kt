package com.techtaurant.mainserver.blog.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.util.Date

/**
 * 블로그 엔티티
 *
 * Soft Delete 적용:
 * - 삭제 시 실제 DB에서 제거되지 않고 deleted_at에 현재 시간 기록
 * - 조회 시 deleted_at IS NULL인 데이터만 자동으로 필터링
 */
@Entity
@Table(name = "blogs")
@SQLDelete(sql = "UPDATE blogs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
class Blog(

    @Column(nullable = false, length = 100, unique = true)
    var name: String,

    @Column(name = "display_name", length = 200)
    var displayName: String?,

    @Column(name = "icon_url", length = 500)
    var iconUrl: String?,

    @Column(name = "base_url", columnDefinition = "TEXT")
    var baseUrl: String?,

    @Column(name = "deleted_at")
    var deletedAt: Date? = null,
) : EntityBase()
