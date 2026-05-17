package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@Transactional
@DisplayName("TagRepository 테스트")
class TagRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var tagRepository: TagRepository

    @Test
    @DisplayName("태그 이름은 중복 저장할 수 없다")
    fun tagNameMustBeUnique() {
        // Given
        val tag = tagRepository.saveAndFlush(Tag(name = "kotlin"))

        // When & Then
        assertThat(tag.id).isNotNull()

        assertThrows<DataIntegrityViolationException> {
            tagRepository.saveAndFlush(Tag(name = "kotlin"))
        }
    }
}
