package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.common.base.EntityBase_
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.entity.Post_
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserBan
import com.techtaurant.mainserver.user.entity.UserBan_
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.util.Calendar
import java.util.UUID

/**
 * 게시물 동적 쿼리 구현체
 *
 * JPA Criteria API와 Metamodel을 사용하여 기간 필터링, 정렬, 커서 기반 페이지네이션을 지원
 */
@Repository
class PostRepositoryCustomImpl : PostRepositoryCustom {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 기간 필터 + 정렬 조건을 적용하여 게시물 목록 조회
     *
     * N+1 방지를 위해 author, category, tags를 fetch join하며 커서 기반 페이지네이션으로 조회
     */
    override fun findPostsWithConditions(
        cursor: PostCursor?,
        size: Int,
        period: PostPeriod,
        sortType: PostSortType,
        authorId: UUID?,
        statuses: List<PostStatusEnum>?,
        categoryId: UUID?,
        visibleToUserId: UUID?,
        tagIds: List<UUID>?,
        viewerId: UUID?,
    ): List<Post> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Post::class.java)
        val root = cq.from(Post::class.java)
        cq.distinct(true)

        root.fetch<Post, User>(Post_.AUTHOR)
        root.fetch<Post, Category>(Post_.CATEGORY, JoinType.LEFT)
        root.fetch<Post, Tag>(Post_.TAGS, JoinType.LEFT)

        val predicates = mutableListOf<Predicate>()

        if (visibleToUserId != null) {
            val publishedPredicate = cb.equal(root.get(Post_.status), PostStatusEnum.PUBLISHED)
            val ownPrivatePostPredicate =
                cb.and(
                    cb.equal(root.get(Post_.author).get(EntityBase_.id), visibleToUserId),
                    cb.equal(root.get(Post_.status), PostStatusEnum.PRIVATE),
                )
            predicates.add(cb.or(publishedPredicate, ownPrivatePostPredicate))
        } else if (statuses != null) {
            predicates.add(root.get(Post_.status).`in`(statuses))
        } else {
            predicates.add(cb.equal(root.get(Post_.status), PostStatusEnum.PUBLISHED))
        }

        authorId?.let { predicates.add(cb.equal(root.get(Post_.author).get(EntityBase_.id), it)) }
        categoryId?.let { predicates.add(cb.equal(root.get(Post_.category).get(EntityBase_.id), it)) }
        tagIds?.let { ids ->
            val tagJoin = root.join<Post, Tag>(Post_.TAGS, JoinType.LEFT)
            predicates.add(tagJoin.get<UUID>("id").`in`(ids))
        }
        addViewerBanCondition(cb, cq, root, viewerId, predicates)

        addPeriodCondition(cb, root, period, predicates)
        addCursorCondition(cb, root, cursor, sortType, predicates)

        if (predicates.isNotEmpty()) {
            cq.where(*predicates.toTypedArray())
        }

        applyOrdering(cb, cq, root, sortType)

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .resultList
            .distinctBy { it.id }
    }

    override fun findVisiblePostDetailById(
        postId: UUID,
        viewerId: UUID?,
    ): Post? {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Post::class.java)
        val root = cq.from(Post::class.java)
        cq.distinct(true)

        root.fetch<Post, User>(Post_.AUTHOR)
        root.fetch<Post, Tag>(Post_.TAGS, JoinType.LEFT)
        root.fetch<Post, Any>(Post_.CATEGORY, JoinType.LEFT)

        val predicates =
            mutableListOf<Predicate>(
                cb.equal(root.get(EntityBase_.id), postId),
            )

        addViewerBanCondition(cb, cq, root, viewerId, predicates)
        cq.where(*predicates.toTypedArray())

        return entityManager.createQuery(cq).resultList.firstOrNull()
    }

    /**
     * 지정된 기간 이내의 게시물만 조회하도록 필터 추가
     *
     * period가 ALL이면 필터 미적용, WEEK/MONTH/YEAR면 해당 일수만큼 이전부터 조회
     */
    private fun addPeriodCondition(
        cb: CriteriaBuilder,
        root: Root<Post>,
        period: PostPeriod,
        predicates: MutableList<Predicate>,
    ) {
        period.days?.let { days ->
            val cutoffDate =
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -days)
                }.time
            predicates.add(cb.greaterThanOrEqualTo(root.get(EntityBase_.createdAt), cutoffDate))
        }
    }

    /**
     * 정렬 타입에 따른 커서 조건 추가
     *
     * 최신순은 updatedAt + id 기준, 그 외(조회/추천/댓글순)는 해당 count + createdAt + id 기준
     */
    private fun addCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Post>,
        cursor: PostCursor?,
        sortType: PostSortType,
        predicates: MutableList<Predicate>,
    ) {
        cursor ?: return

        val cursorPredicate =
            when (sortType) {
                PostSortType.LATEST -> buildLatestCursorCondition(cb, root, cursor)
                else -> buildCountCursorCondition(cb, root, cursor, sortType)
            }
        predicates.add(cursorPredicate)
    }

    private fun addViewerBanCondition(
        cb: CriteriaBuilder,
        cq: CriteriaQuery<Post>,
        root: Root<Post>,
        viewerId: UUID?,
        predicates: MutableList<Predicate>,
    ) {
        if (viewerId == null) {
            return
        }

        val banSubquery = cq.subquery(Long::class.java)
        val banRoot = banSubquery.from(UserBan::class.java)

        banSubquery.select(cb.literal(1L))
        banSubquery.where(
            cb.equal(banRoot.get(UserBan_.user).get(EntityBase_.id), viewerId),
            cb.equal(
                banRoot.get(UserBan_.bannedUser).get(EntityBase_.id),
                root.get(Post_.author).get(EntityBase_.id),
            ),
        )

        predicates.add(cb.not(cb.exists(banSubquery)))
    }

    /**
     * 최신순 커서 조건 생성
     *
     * 최신순은 updatedAt 기준으로 정렬되며, 동일 시간대 게시물 구분을 위해 id를 보조 키로 사용합니다.
     * 조건: (updatedAt < cursor) OR (updatedAt = cursor AND id < cursorId)
     */
    private fun buildLatestCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Post>,
        cursor: PostCursor,
    ): Predicate {
        val updatedAtLess = cb.lessThan(root.get(EntityBase_.updatedAt), cursor.createdAt)
        val updatedAtEqualAndIdLess =
            cb.and(
                cb.equal(root.get(EntityBase_.updatedAt), cursor.createdAt),
                cb.lessThan(root.get(EntityBase_.id), cursor.id),
            )
        return cb.or(updatedAtLess, updatedAtEqualAndIdLess)
    }

    /**
     * count 기반 정렬의 커서 조건 생성 (조회수, 추천수, 댓글수)
     *
     * count가 동일한 게시물 구분을 위해 createdAt, id를 보조 키로 사용
     * 조건: (count < cursor) OR (count = cursor AND createdAt < cursor) OR (count = cursor AND createdAt = cursor AND id < cursorId)
     */
    private fun buildCountCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Post>,
        cursor: PostCursor,
        sortType: PostSortType,
    ): Predicate {
        val sortAttribute =
            when (sortType) {
                PostSortType.VIEW -> Post_.viewCount
                PostSortType.LIKE -> Post_.likeCount
                PostSortType.COMMENT -> Post_.commentCount
                else -> throw ApiException(PostStatus.INVALID_SORT_TYPE)
            }

        val countLess = cb.lessThan(root.get(sortAttribute), cursor.sortValue)
        val countEqualCreatedAtLess =
            cb.and(
                cb.equal(root.get(sortAttribute), cursor.sortValue),
                cb.lessThan(root.get(EntityBase_.createdAt), cursor.createdAt),
            )
        val countEqualCreatedAtEqualIdLess =
            cb.and(
                cb.equal(root.get(sortAttribute), cursor.sortValue),
                cb.equal(root.get(EntityBase_.createdAt), cursor.createdAt),
                cb.lessThan(root.get(EntityBase_.id), cursor.id),
            )
        return cb.or(countLess, countEqualCreatedAtLess, countEqualCreatedAtEqualIdLess)
    }

    /**
     * 정렬 타입에 따른 ORDER BY 절 적용
     *
     * 모든 정렬은 DESC이며 동점자 처리를 위해 createdAt, id를 보조 정렬 키로 추가합니다.
     * 최신순(LATEST)은 updatedAt 기준입니다. createdAt을 기준으로 하면 한 번 생성된 게시물이
     * 영구히 낮은 순위에 머물 수 있으므로, 수정된 게시물도 상단에 노출될 수 있도록 updatedAt을 사용합니다.
     */
    private fun applyOrdering(
        cb: CriteriaBuilder,
        cq: CriteriaQuery<Post>,
        root: Root<Post>,
        sortType: PostSortType,
    ) {
        val orders =
            when (sortType) {
                PostSortType.LATEST ->
                    listOf(
                        cb.desc(root.get(EntityBase_.updatedAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                PostSortType.VIEW ->
                    listOf(
                        cb.desc(root.get(Post_.viewCount)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                PostSortType.LIKE ->
                    listOf(
                        cb.desc(root.get(Post_.likeCount)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                PostSortType.COMMENT ->
                    listOf(
                        cb.desc(root.get(Post_.commentCount)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
            }
        cq.orderBy(orders)
    }
}
