package com.techtaurant.mainserver.user.application

import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

@Service
class BannedUserMaskingService {
    fun maskAuthorId(authorId: UUID): UUID {
        return UUID.nameUUIDFromBytes("banned:$authorId".toByteArray(StandardCharsets.UTF_8))
    }

    fun maskAuthorName(authorId: UUID): String {
        return bannedValue(authorId.toString())
    }

    fun maskCommentContent(commentId: UUID): String {
        return bannedValue(commentId.toString())
    }

    private fun bannedValue(source: String): String {
        return "banned_${hashPrefix(source)}"
    }

    private fun hashPrefix(source: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(source.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(6)
    }
}
