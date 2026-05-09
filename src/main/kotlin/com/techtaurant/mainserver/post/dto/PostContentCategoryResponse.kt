package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Category
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "게시물 콘텐츠 카테고리 응답")
data class PostContentCategoryResponse(
    @field:Schema(description = "카테고리 ID")
    val id: UUID,
    @field:Schema(description = "카테고리 이름", example = "spring")
    val name: String,
    @field:Schema(description = "전체 경로", example = "java/spring/deepdive")
    val path: String,
    @field:Schema(description = "depth (1~5)", example = "2")
    val depth: Int,
    @field:Schema(description = "부모 카테고리 ID (최상위인 경우 null)")
    val parentId: UUID?,
) {
    companion object {
        fun from(category: Category): PostContentCategoryResponse =
            PostContentCategoryResponse(
                id = category.id!!,
                name = category.name,
                path = category.path,
                depth = category.depth,
                parentId = category.parent?.id,
            )
    }
}
