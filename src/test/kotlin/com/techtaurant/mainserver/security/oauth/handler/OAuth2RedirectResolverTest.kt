package com.techtaurant.mainserver.security.oauth.handler

import com.techtaurant.mainserver.security.config.CorsProperties
import com.techtaurant.mainserver.security.helper.CookieHelper
import com.techtaurant.mainserver.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class OAuth2RedirectResolverTest {
    private val cookieHelper: CookieHelper = mockk()
    private lateinit var resolver: OAuth2RedirectResolver
    private lateinit var request: MockHttpServletRequest

    @BeforeEach
    fun setUp() {
        resolver =
            OAuth2RedirectResolver(
                cookieHelper = cookieHelper,
                corsProperties =
                    CorsProperties(
                        allowedOrigins = "https://techtaurant.com,http://localhost:3000",
                    ),
            )
        request = MockHttpServletRequest()
    }

    @Test
    @DisplayName("성공 redirect-uri가 내부 path이면 허용된 origin과 결합한다")
    fun `resolve success redirect with internal path`() {
        // given
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )
        } returns "https://techtaurant.com"
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_SUCCESS_REDIRECT_URI_COOKIE,
            )
        } returns "/ko/oauth/callback?redirect=%2Fko%2Fpost%2Fwrite"

        // when
        val redirectUrl = resolver.resolveSuccessRedirectUrl(request)

        // then
        assertThat(redirectUrl).isEqualTo(
            "https://techtaurant.com/ko/oauth/callback?redirect=%2Fko%2Fpost%2Fwrite",
        )
    }

    @Test
    @DisplayName("실패 redirect-uri가 허용된 absolute URL이면 그대로 사용한다")
    fun `resolve failure redirect with allowed absolute url`() {
        // given
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )
        } returns "https://techtaurant.com"
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_FAILURE_REDIRECT_URI_COOKIE,
            )
        } returns "http://localhost:3000/ko/oauth/error"

        // when
        val redirectUrl = resolver.resolveFailureRedirectUrl(request)

        // then
        assertThat(redirectUrl).isEqualTo("http://localhost:3000/ko/oauth/error")
    }

    @Test
    @DisplayName("redirect-uri absolute URL의 origin이 허용되지 않으면 기본 실패 경로로 fallback한다")
    fun `fallback failure redirect when absolute url origin is not allowed`() {
        // given
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )
        } returns "https://techtaurant.com"
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_FAILURE_REDIRECT_URI_COOKIE,
            )
        } returns "https://evil.example/ko/oauth/error"

        // when
        val redirectUrl = resolver.resolveFailureRedirectUrl(request)

        // then
        assertThat(redirectUrl).isEqualTo("https://techtaurant.com/oauth/error")
    }

    @Test
    @DisplayName("origin 쿠키가 허용되지 않으면 첫 번째 허용 origin으로 내부 path를 결합한다")
    fun `fallback origin when origin cookie is not allowed`() {
        // given
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )
        } returns "https://evil.example"
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_SUCCESS_REDIRECT_URI_COOKIE,
            )
        } returns "/en/oauth/callback"

        // when
        val redirectUrl = resolver.resolveSuccessRedirectUrl(request)

        // then
        assertThat(redirectUrl).isEqualTo("https://techtaurant.com/en/oauth/callback")
    }

    @Test
    @DisplayName("성공 redirect-uri가 없으면 기본 성공 경로로 fallback한다")
    fun `fallback success redirect when redirect uri is missing`() {
        // given
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )
        } returns "https://techtaurant.com"
        every {
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_SUCCESS_REDIRECT_URI_COOKIE,
            )
        } returns null

        // when
        val redirectUrl = resolver.resolveSuccessRedirectUrl(request)

        // then
        assertThat(redirectUrl).isEqualTo("https://techtaurant.com/oauth/callback")
    }
}
