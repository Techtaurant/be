package com.techtaurant.mainserver.batchlog.entity

import com.techtaurant.mainserver.batchlog.enums.BatchStatus
import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "batch_log")
class BatchLog(

    @Column(name = "batch_name", nullable = false, length = 100)
    var batchName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: BatchStatus,

    @Column(name = "started_at", nullable = false)
    var startedAt: Date,

    @Column(name = "finished_at")
    var finishedAt: Date? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "screenshot_base64", columnDefinition = "TEXT")
    var screenshotBase64: String? = null,
) : EntityBase()
