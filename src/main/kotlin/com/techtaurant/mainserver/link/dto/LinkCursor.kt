package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.link.entity.Link
import java.util.Date
import java.util.UUID

data class LinkCursor(
    val createdAt: Date,
    val id: UUID,
) {
    fun encode(): String = "${createdAt.time}$CURSOR_DELIMITER$id"

    companion object {
        private const val CURSOR_DELIMITER = "_"

        fun decode(cursor: String): LinkCursor? =
            runCatching {
                val parts = cursor.split(CURSOR_DELIMITER, limit = 2)
                require(parts.size == 2)
                LinkCursor(Date(parts[0].toLong()), UUID.fromString(parts[1]))
            }.getOrNull()

        fun from(link: Link): LinkCursor = LinkCursor(link.createdAt, link.id!!)
    }
}
