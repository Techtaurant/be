package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.common.base.EntityBase_
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.dto.PostCursor
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.entity.PostDailyStats_
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
import jakarta.persistence.criteria.CommonAbstractCriteria
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.sql.Date as SqlDate
import java.util.Date as UtilDate

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
        if (usesDailyStatsPeriodRanking(period, sortType)) {
            return findPostsWithDailyStatsPeriodRanking(
                cursor = cursor,
                size = size,
                period = period,
                sortType = sortType,
                authorId = authorId,
                statuses = statuses,
                categoryId = categoryId,
                visibleToUserId = visibleToUserId,
                tagIds = tagIds,
                viewerId = viewerId,
            )
        }

        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Post::class.java)
        val root = cq.from(Post::class.java)
        cq.distinct(true)

        root.fetch<Post, User>(Post_.AUTHOR)
        root.fetch<Post, Category>(Post_.CATEGORY, JoinType.LEFT)
        root.fetch<Post, Tag>(Post_.TAGS, JoinType.LEFT)

        val predicates = mutableListOf<Predicate>()
        addBaseConditions(
            cb = cb,
            query = cq,
            root = root,
            authorId = authorId,
            statuses = statuses,
            categoryId = categoryId,
            visibleToUserId = visibleToUserId,
            tagIds = tagIds,
            viewerId = viewerId,
            predicates = predicates,
        )
        addPeriodCondition(cb, cq, root, period, sortType, predicates)
        addCursorCondition(cb, cq, root, cursor, period, sortType, predicates)

        if (predicates.isNotEmpty()) {
            cq.where(*predicates.toTypedArray())
        }

        applyOrdering(cb, cq, root, period, sortType)

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .resultList
            .distinctBy { it.id }
    }

    private fun findPostsWithDailyStatsPeriodRanking(
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
        val cq = cb.createQuery(UUID::class.java)
        val root = cq.from(Post::class.java)
        val predicates = mutableListOf<Predicate>()

        addBaseConditions(
            cb = cb,
            query = cq,
            root = root,
            authorId = authorId,
            statuses = statuses,
            categoryId = categoryId,
            visibleToUserId = visibleToUserId,
            tagIds = tagIds,
            viewerId = viewerId,
            predicates = predicates,
        )
        addPeriodCondition(cb, cq, root, period, sortType, predicates)
        addCursorCondition(cb, cq, root, cursor, period, sortType, predicates)

        cq.select(root.get(EntityBase_.id))
        if (predicates.isNotEmpty()) {
            cq.where(*predicates.toTypedArray())
        }
        applyOrdering(cb, cq, root, period, sortType)

        val postIds =
            entityManager.createQuery(cq)
                .setMaxResults(size)
                .resultList

        return findPostsByIdsInOrder(postIds)
    }

    private fun findPostsByIdsInOrder(postIds: List<UUID>): List<Post> {
        if (postIds.isEmpty()) {
            return emptyList()
        }

        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Post::class.java)
        val root = cq.from(Post::class.java)
        cq.distinct(true)

        root.fetch<Post, User>(Post_.AUTHOR)
        root.fetch<Post, Category>(Post_.CATEGORY, JoinType.LEFT)
        root.fetch<Post, Tag>(Post_.TAGS, JoinType.LEFT)
        cq.where(root.get<UUID>(EntityBase_.id).`in`(postIds))

        val orderByPostId = postIds.withIndex().associate { (index, postId) -> postId to index }
        return entityManager.createQuery(cq)
            .resultList
            .distinctBy { it.id }
            .sortedBy { orderByPostId.getValue(it.id!!) }
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

    private fun addBaseConditions(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        authorId: UUID?,
        statuses: List<PostStatusEnum>?,
        categoryId: UUID?,
        visibleToUserId: UUID?,
        tagIds: List<UUID>?,
        viewerId: UUID?,
        predicates: MutableList<Predicate>,
    ) {
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
        tagIds?.let { addTagIdsCondition(cb, query, root, it, predicates) }
        addViewerBanCondition(cb, query, root, viewerId, predicates)
    }

    private fun addTagIdsCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        tagIds: List<UUID>,
        predicates: MutableList<Predicate>,
    ) {
        val tagSubquery = query.subquery(Long::class.java)
        val tagPostRoot = tagSubquery.from(Post::class.java)
        val tagJoin = tagPostRoot.join<Post, Tag>(Post_.TAGS, JoinType.INNER)

        tagSubquery.select(cb.literal(1L))
        tagSubquery.where(
            cb.equal(tagPostRoot.get(EntityBase_.id), root.get(EntityBase_.id)),
            tagJoin.get<UUID>("id").`in`(tagIds),
        )
        predicates.add(cb.exists(tagSubquery))
    }

    /**
     * 지정된 기간 조건을 적용합니다.
     *
     * LATEST 정렬은 기존처럼 게시물 작성일 기준으로 필터링하고,
     * VIEW/LIKE/COMMENT 정렬은 게시물 작성일이 아니라 기간 내 일별 통계 존재 여부로 필터링합니다.
     */
    private fun addPeriodCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        period: PostPeriod,
        sortType: PostSortType,
        predicates: MutableList<Predicate>,
    ) {
        if (sortType == PostSortType.LATEST) {
            addPostCreatedAtPeriodCondition(cb, root, period, predicates)
            return
        }
        addDailyStatsPeriodCondition(cb, query, root, period, predicates)
    }

    /**
     * LATEST 정렬의 기간 필터는 게시물 작성일 기준을 유지합니다.
     */
    private fun addPostCreatedAtPeriodCondition(
        cb: CriteriaBuilder,
        root: Root<Post>,
        period: PostPeriod,
        predicates: MutableList<Predicate>,
    ) {
        period.days?.let { days ->
            val cutoffDate = UtilDate.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS))
            predicates.add(cb.greaterThanOrEqualTo(root.get(EntityBase_.createdAt), cutoffDate))
        }
    }

    /**
     * count 기반 기간 랭킹은 게시물 작성일이 아니라 일별 통계 테이블의 최근 데이터 존재 여부로 필터링합니다.
     */
    private fun addDailyStatsPeriodCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        period: PostPeriod,
        predicates: MutableList<Predicate>,
    ) {
        val days = period.days ?: return
        val statsExistsSubquery = query.subquery(Long::class.java)
        val statsRoot = statsExistsSubquery.from(PostDailyStats::class.java)

        statsExistsSubquery.select(cb.literal(1L))
        statsExistsSubquery.where(
            cb.equal(statsRoot.get(PostDailyStats_.post).get(EntityBase_.id), root.get(EntityBase_.id)),
            cb.greaterThanOrEqualTo(statsRoot.get(PostDailyStats_.statDate), statsCutoffDate(days)),
        )
        predicates.add(cb.exists(statsExistsSubquery))
    }

    /**
     * 정렬 타입에 따른 커서 조건 추가
     *
     * 최신순은 updatedAt + id 기준, 그 외(조회/추천/댓글순)는 해당 count + createdAt + id 기준
     */
    private fun addCursorCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        cursor: PostCursor?,
        period: PostPeriod,
        sortType: PostSortType,
        predicates: MutableList<Predicate>,
    ) {
        cursor ?: return

        val cursorPredicate =
            when (sortType) {
                PostSortType.LATEST -> buildLatestCursorCondition(cb, root, cursor)
                else -> buildCountCursorCondition(cb, query, root, cursor, period, sortType)
            }
        predicates.add(cursorPredicate)
    }

    private fun addViewerBanCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        viewerId: UUID?,
        predicates: MutableList<Predicate>,
    ) {
        if (viewerId == null) {
            return
        }

        val banSubquery = query.subquery(Long::class.java)
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
        query: CommonAbstractCriteria,
        root: Root<Post>,
        cursor: PostCursor,
        period: PostPeriod,
        sortType: PostSortType,
    ): Predicate {
        val sortExpression = countSortExpression(cb, query, root, period, sortType)

        val countLess = cb.lessThan(sortExpression, cursor.sortValue)
        val countEqualCreatedAtLess =
            cb.and(
                cb.equal(sortExpression, cursor.sortValue),
                cb.lessThan(root.get(EntityBase_.createdAt), cursor.createdAt),
            )
        val countEqualCreatedAtEqualIdLess =
            cb.and(
                cb.equal(sortExpression, cursor.sortValue),
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
        cq: CriteriaQuery<*>,
        root: Root<Post>,
        period: PostPeriod,
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
                        cb.desc(countSortExpression(cb, cq, root, period, sortType)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                PostSortType.LIKE ->
                    listOf(
                        cb.desc(countSortExpression(cb, cq, root, period, sortType)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                PostSortType.COMMENT ->
                    listOf(
                        cb.desc(countSortExpression(cb, cq, root, period, sortType)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
            }
        cq.orderBy(orders)
    }

    private fun countSortExpression(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        period: PostPeriod,
        sortType: PostSortType,
    ): Expression<Long> {
        return if (usesDailyStatsPeriodRanking(period, sortType)) {
            dailyStatsSumExpression(cb, query, root, period.days!!, sortType)
        } else {
            postCountExpression(root, sortType)
        }
    }

    private fun dailyStatsSumExpression(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Post>,
        days: Int,
        sortType: PostSortType,
    ): Expression<Long> {
        val sumSubquery = query.subquery(Long::class.java)
        val statsRoot = sumSubquery.from(PostDailyStats::class.java)

        sumSubquery.select(cb.coalesce(cb.sum(dailyStatsCountExpression(statsRoot, sortType)), 0L))
        sumSubquery.where(
            cb.equal(statsRoot.get(PostDailyStats_.post).get(EntityBase_.id), root.get(EntityBase_.id)),
            cb.greaterThanOrEqualTo(statsRoot.get(PostDailyStats_.statDate), statsCutoffDate(days)),
        )
        return sumSubquery
    }

    private fun postCountExpression(
        root: Root<Post>,
        sortType: PostSortType,
    ): Path<Long> =
        when (sortType) {
            PostSortType.VIEW -> root.get(Post_.viewCount)
            PostSortType.LIKE -> root.get(Post_.likeCount)
            PostSortType.COMMENT -> root.get(Post_.commentCount)
            else -> throw ApiException(PostStatus.INVALID_SORT_TYPE)
        }

    private fun dailyStatsCountExpression(
        statsRoot: Root<PostDailyStats>,
        sortType: PostSortType,
    ): Path<Long> =
        when (sortType) {
            PostSortType.VIEW -> statsRoot.get(PostDailyStats_.viewCount)
            PostSortType.LIKE -> statsRoot.get(PostDailyStats_.likeCount)
            PostSortType.COMMENT -> statsRoot.get(PostDailyStats_.commentCount)
            else -> throw ApiException(PostStatus.INVALID_SORT_TYPE)
        }

    private fun usesDailyStatsPeriodRanking(
        period: PostPeriod,
        sortType: PostSortType,
    ): Boolean = sortType != PostSortType.LATEST && period.days != null

    private fun statsCutoffDate(days: Int): SqlDate = SqlDate.valueOf(LocalDate.now(ZoneOffset.UTC).minusDays(days.toLong()))
}
