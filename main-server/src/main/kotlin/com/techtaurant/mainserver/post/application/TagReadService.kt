package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.dto.TagResponse
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.post.infrastructure.out.TagWithPostCountProjection
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TagReadService(
    private val tagRepository: TagRepository,
) {
    companion object {
        private const val CURSOR_DELIMITER = "_"
    }

    fun getTagsWithPostCount(name: String?, cursor: String?, size: Int): CursorPageResponse<TagResponse> {
        val limit = size + 1

        val searchedTags = if (cursor == null) {
            tagRepository.findAllWithPostCount(name, limit)
        } else {
            val (lastPostCount, lastTagId) = parseCursor(cursor)
            tagRepository.findAllWithPostCountAfterCursor(name, lastPostCount, lastTagId, limit)
        }

        val hasNext = searchedTags.size > size
        val searchedTagResponses = searchedTags.take(size).map { it.toResponse() }

        val nextCursor = if (hasNext && searchedTagResponses.isNotEmpty()) {
            val lastItem = searchedTagResponses.last()
            "${lastItem.postCount}$CURSOR_DELIMITER${lastItem.id}"
        } else {
            null
        }

        return CursorPageResponse(
            content = searchedTagResponses,
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = searchedTagResponses.size,
        )
    }

    /**
     * 커서 파싱: "{postCount}_{tagId}" → (lastPostCount, lastTagId)
     *
     * 커서는 마지막으로 조회한 태그의 정보를 담고 있으며,
     * 다음 페이지 조회 시 이 값보다 뒤에 있는 항목들을 가져옴
     */
    private fun parseCursor(cursor: String): Pair<Long, java.util.UUID> {
        val parts = cursor.split(CURSOR_DELIMITER, limit = 2)
        require(parts.size == 2) { "Invalid cursor format" }
        return Pair(parts[0].toLong(), java.util.UUID.fromString(parts[1]))
    }

    private fun TagWithPostCountProjection.toResponse() = TagResponse(
        id = getId(),
        name = getName(),
        postCount = getPostCount(),
    )
}
