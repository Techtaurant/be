package com.techtaurant.mainserver.link.infrastructure.out

import java.util.UUID

interface LinkStatsAggregateProjection {
    fun getLinkId(): UUID

    fun getViewCount(): Long

    fun getLikeCount(): Long

    fun getSaveCount(): Long
}
