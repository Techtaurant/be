package com.techtaurant.mainserver.batchlog.infrastructure.out

import com.techtaurant.mainserver.batchlog.entity.BatchLog
import com.techtaurant.mainserver.batchlog.enums.BatchStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BatchLogRepository : JpaRepository<BatchLog, UUID> {
    fun findByBatchName(batchName: String): List<BatchLog>
    fun findByStatus(status: BatchStatus): List<BatchLog>
}
