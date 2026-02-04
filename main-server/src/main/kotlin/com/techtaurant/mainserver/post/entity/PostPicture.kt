package com.techtaurant.mainserver.post.entity

import com.techtaurant.mainserver.common.base.EntityBase
import jakarta.persistence.*

/**
 * 게시물 사진 엔티티
 *
 * 게시물에 첨부된 사진 정보를 저장합니다.
 * is_thumbnail 플래그를 통해 대표 썸네일을 지정할 수 있으며,
 * display_order를 통해 사진의 표시 순서를 관리합니다.
 *
 * @property post 사진이 속한 게시물 (Post와 N:1 관계)
 * @property pictureUrl 사진 파일 URL (최대 500자)
 * @property isThumbnail 대표 썸네일 여부 (기본값: false)
 * @property displayOrder 사진 표시 순서 (기본값: 0, 낮을수록 우선)
 */
@Entity
@Table(name = "post_pictures")
class PostPicture(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @Column(name = "picture_url", nullable = false, length = 500)
    var pictureUrl: String,

    @Column(name = "is_thumbnail", nullable = false)
    var isThumbnail: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

) : EntityBase()
