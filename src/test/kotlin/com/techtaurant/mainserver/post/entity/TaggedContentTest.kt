package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.enums.TagStatus
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFailsWith

@DisplayName("TaggedContent 테스트")
class TaggedContentTest {
    @Test
    @DisplayName("게시물은 태그를 10개까지만 가질 수 있다")
    fun postTags_cannotExceedTenTags() {
        val author = createUser()

        val exception =
            assertFailsWith<ApiException> {
                Post(
                    title = "게시물",
                    content = "본문",
                    author = author,
                    tags = createTags(11).toMutableSet(),
                    status = PostStatusEnum.PUBLISHED,
                )
            }

        assertThat(exception.status).isEqualTo(TagStatus.TAG_COUNT_EXCEEDED)
    }

    @Test
    @DisplayName("링크는 태그를 10개까지만 가질 수 있다")
    fun linkTags_cannotExceedTenTags() {
        val exception =
            assertFailsWith<ApiException> {
                Link(
                    title = "링크",
                    url = "https://example.com/link",
                    summary = "요약",
                    tags = createTags(11).toMutableSet(),
                )
            }

        assertThat(exception.status).isEqualTo(TagStatus.TAG_COUNT_EXCEEDED)
    }

    @Test
    @DisplayName("공통 태그 교체 메서드는 10개 이하 태그만 반영한다")
    fun replaceTags_allowsUpToTenTags() {
        val post =
            Post(
                title = "게시물",
                content = "본문",
                author = createUser(),
            )

        post.replaceTags(createTags(10))

        assertThat(post.tags).hasSize(10)
    }

    private fun createTags(count: Int): List<Tag> =
        (1..count).map { index ->
            Tag(name = "tag-$index")
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
