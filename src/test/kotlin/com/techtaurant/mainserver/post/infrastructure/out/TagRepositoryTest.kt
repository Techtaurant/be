package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@DisplayName("TagRepository 테스트")
class TagRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var tagRepository: TagRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userRepository: UserRepository

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

    @Test
    @DisplayName("게시물에 연결되지 않은 링크 전용 태그는 게시물 태그 목록에서 제외된다")
    fun findAllWithPostCount_excludesLinkOnlyTags() {
        // Given
        val author = userRepository.save(createUser())
        val postTag = tagRepository.saveAndFlush(Tag(name = "post-tag-${UUID.randomUUID()}"))
        val linkOnlyTag = tagRepository.saveAndFlush(Tag(name = "link-only-tag-${UUID.randomUUID()}"))
        postRepository.saveAndFlush(
            Post(
                title = "게시물",
                content = "본문",
                author = author,
                status = PostStatusEnum.PUBLISHED,
            ).apply { replaceTags(setOf(postTag)) },
        )
        linkRepository.saveAndFlush(
            Link(
                title = "링크",
                url = "https://example.com/${UUID.randomUUID()}",
                summary = "요약",
            ).apply { replaceTags(setOf(linkOnlyTag)) },
        )

        // When
        val firstPageTagNames = tagRepository.findAllWithPostCount(null, 10).map { it.getName() }
        val linkOnlySearchResults = tagRepository.findAllWithPostCount(linkOnlyTag.name, 10)
        val cursorPageTagNames =
            tagRepository.findAllWithPostCountAfterCursor(
                name = null,
                lastPostCount = 1,
                lastTagId = postTag.id!!,
                limit = 10,
            ).map { it.getName() }

        // Then
        assertThat(firstPageTagNames).contains(postTag.name)
        assertThat(firstPageTagNames).doesNotContain(linkOnlyTag.name)
        assertThat(linkOnlySearchResults).isEmpty()
        assertThat(cursorPageTagNames).doesNotContain(linkOnlyTag.name)
    }

    private fun createUser(): User =
        User(
            name = "작성자-${UUID.randomUUID()}",
            email = "${UUID.randomUUID()}@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = UUID.randomUUID().toString(),
            role = UserRole.USER,
            profileImageUrl = "https://example.com/profile.jpg",
        )
}
