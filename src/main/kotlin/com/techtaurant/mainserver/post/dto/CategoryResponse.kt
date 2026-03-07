package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Category
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 카테고리 응답 DTO
 *
 * @property id 카테고리 고유 ID
 * @property name 카테고리 이름
 * @property path 전체 경로 (예: "java/spring/deepdive")
 * @property depth 현재 depth (1~5)
 * @property parentId 부모 카테고리 ID (최상위 카테고리인 경우 null)
 */
@Schema(description = "카테고리 응답")
data class CategoryResponse(
    @Schema(description = "카테고리 ID")
    val id: UUID,
    @Schema(description = "카테고리 이름", example = "spring")
    val name: String,
    @Schema(description = "전체 경로", example = "java/spring/deepdive")
    val path: String,
    @Schema(description = "depth (1~5)", example = "2")
    val depth: Int,
    @Schema(description = "부모 카테고리 ID (최상위인 경우 null)")
    val parentId: UUID?,
) {
    companion object {
        fun from(category: Category) =
            CategoryResponse(
                id = category.id!!,
                name = category.name,
                path = category.path,
                depth = category.depth,
                parentId = category.parent?.id,
            )
    }
}
