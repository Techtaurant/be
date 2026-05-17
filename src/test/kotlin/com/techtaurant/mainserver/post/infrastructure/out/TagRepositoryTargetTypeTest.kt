package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.enums.TagTargetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
@DisplayName("TagRepository 태그 타입 분리 테스트")
class TagRepositoryTargetTypeTest : IntegrationTest() {
    @Autowired
    private lateinit var tagRepository: TagRepository

    @Test
    @DisplayName("같은 이름이어도 대상 타입이 다르면 각각 저장할 수 있다")
    fun tagsWithSameNameCanExistWhenTargetTypeDiffers() {
        // Given
        val postTag = tagRepository.save(Tag(name = "kotlin", targetType = TagTargetType.POST))
        val linkTag = tagRepository.saveAndFlush(Tag(name = "kotlin", targetType = TagTargetType.LINK))

        // When
        val searchedPostTag = tagRepository.findByNameAndTargetType("kotlin", TagTargetType.POST)
        val searchedLinkTag = tagRepository.findByNameAndTargetType("kotlin", TagTargetType.LINK)

        // Then
        assertThat(postTag.id).isNotNull()
        assertThat(linkTag.id).isNotNull()
        assertThat(searchedPostTag?.id).isEqualTo(postTag.id)
        assertThat(searchedLinkTag?.id).isEqualTo(linkTag.id)
    }
}
