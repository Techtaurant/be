package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import java.util.Base64
import java.util.Date
import java.util.UUID

data class NotificationCursor(
    val createdAt: Date,
    val id: UUID,
) {
    fun encode(): String {
        val raw = "${createdAt.time}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    companion object {
        fun decode(cursor: String): NotificationCursor? {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(cursor))
                val parts = decoded.split(":")
                if (parts.size != 2) {
                    return null
                }

                val timestamp = parts[0].toLongOrNull() ?: return null
                val id = UUID.fromString(parts[1])
                NotificationCursor(createdAt = Date(timestamp), id = id)
            } catch (exception: Exception) {
                null
            }
        }

        fun from(recipient: NotificationRecipient): NotificationCursor = NotificationCursor(recipient.createdAt, recipient.id!!)
    }
}
