package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class NotificationCursor(
    val createdAt: Instant,
    val id: UUID,
) {
    fun encode(): String {
        val raw = "$createdAt|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    companion object {
        fun decode(cursor: String): NotificationCursor? {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(cursor))
                decodePipeDelimited(decoded) ?: decodeLegacyColonDelimited(decoded)
            } catch (exception: Exception) {
                null
            }
        }

        private fun decodePipeDelimited(decoded: String): NotificationCursor? {
            val parts = decoded.split("|")
            if (parts.size != 2) return null

            val createdAt = Instant.parse(parts[0])
            val id = UUID.fromString(parts[1])

            return NotificationCursor(createdAt = createdAt, id = id)
        }

        private fun decodeLegacyColonDelimited(decoded: String): NotificationCursor? {
            val parts = decoded.split(":")
            if (parts.size != 2) return null

            val timestamp = parts[0].toLongOrNull() ?: return null
            val id = UUID.fromString(parts[1])

            return NotificationCursor(createdAt = Instant.ofEpochMilli(timestamp), id = id)
        }

        fun from(recipient: NotificationRecipient): NotificationCursor = NotificationCursor(recipient.createdAt, recipient.id!!)
    }
}
