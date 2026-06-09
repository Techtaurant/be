package com.techtaurant.mainserver.common.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class EntityBase(
    @Id
    @UuidV7
    @Column(columnDefinition = "UUID")
    var id: UUID? = null,
    @CreatedDate
    @Column(name = "created_at_utc")
    var createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column(name = "updated_at_utc")
    var updatedAt: Instant = Instant.now(),
)
