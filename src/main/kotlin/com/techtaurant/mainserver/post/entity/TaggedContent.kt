package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.enums.TagStatus

interface TaggedContent {
    var tags: MutableSet<Tag>

    fun replaceTags(tags: Collection<Tag>) {
        validateTagCount(tags)
        this.tags = tags.toMutableSet()
    }

    fun validateTagCount() {
        validateTagCount(tags)
    }

    companion object {
        const val MAX_TAG_COUNT = 10

        fun validateTagCount(tags: Collection<Tag>) {
            if (tags.size > MAX_TAG_COUNT) {
                throw ApiException(TagStatus.TAG_COUNT_EXCEEDED)
            }
        }

        fun validateTagNameCount(tagNames: Collection<String>) {
            if (tagNames.size > MAX_TAG_COUNT) {
                throw ApiException(TagStatus.TAG_COUNT_EXCEEDED)
            }
        }
    }
}
