package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.lock.DistributedLock
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.entity.TaggedContent
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import org.springframework.stereotype.Service

@Service
class TagWriteService(
    private val tagRepository: TagRepository,
    private val distributedLock: DistributedLock,
) {
    fun resolveTags(tagNames: Collection<String>): Set<Tag> {
        val normalizedNames =
            tagNames
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()

        TaggedContent.validateTagNameCount(normalizedNames)

        if (normalizedNames.isEmpty()) {
            return emptySet()
        }

        val existingTags = tagRepository.findByNameIn(normalizedNames)
        val existingTagNames = existingTags.map { it.name }.toSet()
        val newTags =
            normalizedNames
                .filter { it !in existingTagNames }
                .map { tagName ->
                    distributedLock.withLockAndTransaction("tag:$tagName") {
                        tagRepository.findByName(tagName)
                            ?: tagRepository.save(Tag(name = tagName))
                    }
                }

        return (existingTags + newTags).toSet()
    }
}
