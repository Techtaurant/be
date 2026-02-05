package com.techtaurant.mainserver.common.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.util.Date
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class EntityBase(
    @Id
    @UuidV7
    @Column(columnDefinition = "UUID")
    var id: UUID? = null,

    @CreatedDate
    @Column(name = "created_at")
    var createdAt: Date = Date(),

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: Date = Date(),
)
