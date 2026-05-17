package com.techtaurant.mainserver.common.swagger

import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.security.oauth.status.OAuthStatus
import com.techtaurant.mainserver.user.enums.UserStatus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorResponses(
    val comments: Array<CommentStatus> = [],
    val defaults: Array<DefaultStatus> = [],
    val jwts: Array<JwtStatus> = [],
    val oauths: Array<OAuthStatus> = [],
    val posts: Array<PostStatus> = [],
    val users: Array<UserStatus> = [],
    val includeAuthenticationErrors: Boolean = false,
    val includeValidationError: Boolean = false,
)
