package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.common.base.EntityBase_
import com.techtaurant.mainserver.link.dto.LinkCursorV1
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.entity.LinkDailyStats_
import com.techtaurant.mainserver.link.entity.Link_
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.entity.UserLink_
import com.techtaurant.mainserver.link.enums.LinkPeriod
import com.techtaurant.mainserver.link.enums.LinkSortType
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.entity.Tag_
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CommonAbstractCriteria
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * 공개 링크 동적 정렬/페이지네이션 구현체 (v1)
 *
 * JPA Criteria API와 Metamodel을 사용해 정렬 타입별 키셋 커서 페이지네이션을 지원합니다.
 * - PUBLISHED: 발행일 기준 정렬 (발행일 없는 링크는 뒤로)
 * - LIKE/SAVE: LinkDailyStats를 기간 윈도우로 집계한 합 기준 정렬
 */
@Repository
class LinkRepositoryCustomImpl : LinkRepositoryCustom {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findPublicLinkIds(
        cursor: LinkCursorV1?,
        limit: Int,
        sortType: LinkSortType,
        period: LinkPeriod,
        sourceCompanyUserId: UUID?,
        tag: String?,
    ): List<RankedLinkId> =
        when (sortType) {
            LinkSortType.PUBLISHED -> findPublishedRankedIds(cursor, limit, sourceCompanyUserId, tag)
            LinkSortType.LIKE, LinkSortType.SAVE ->
                findStatsRankedIds(cursor, limit, sortType, period, sourceCompanyUserId, tag)
        }

    private fun findPublishedRankedIds(
        cursor: LinkCursorV1?,
        limit: Int,
        sourceCompanyUserId: UUID?,
        tag: String?,
    ): List<RankedLinkId> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(UUID::class.java)
        val root = cq.from(Link::class.java)
        val predicates = mutableListOf<Predicate>()

        addBaseConditions(cb, cq, root, sourceCompanyUserId, tag, predicates)
        cursor?.let { predicates.add(buildPublishedCursorCondition(cb, root, it)) }

        cq.select(root.get(EntityBase_.id))
        if (predicates.isNotEmpty()) {
            cq.where(*predicates.toTypedArray())
        }
        cq.orderBy(publishedSortOrders(cb, root))

        return entityManager.createQuery(cq)
            .setMaxResults(limit)
            .resultList
            .map { RankedLinkId(linkId = it, sortValue = 0L) }
    }

    private fun findStatsRankedIds(
        cursor: LinkCursorV1?,
        limit: Int,
        sortType: LinkSortType,
        period: LinkPeriod,
        sourceCompanyUserId: UUID?,
        tag: String?,
    ): List<RankedLinkId> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createTupleQuery()
        val root = cq.from(Link::class.java)
        val statsRoot = cq.from(LinkDailyStats::class.java)
        val idPath = root.get<UUID>(EntityBase_.id)
        val createdAtPath = root.get<Instant>(EntityBase_.createdAt)
        val sortExpression = dailyStatsSumExpression(cb, statsRoot, sortType)
        val predicates = mutableListOf<Predicate>()

        addBaseConditions(cb, cq, root, sourceCompanyUserId, tag, predicates)
        addStatsJoinCondition(cb, root, statsRoot, period, predicates)

        cq.multiselect(idPath, sortExpression)
        cq.where(*predicates.toTypedArray())
        cq.groupBy(idPath, createdAtPath)
        cursor?.let { cq.having(buildCountCursorCondition(cb, root, it, sortExpression)) }
        cq.orderBy(countSortOrders(cb, root, sortExpression))

        return entityManager.createQuery(cq)
            .setMaxResults(limit)
            .resultList
            .map { it.toRankedLinkId() }
    }

    private fun Tuple.toRankedLinkId(): RankedLinkId =
        RankedLinkId(
            linkId = get(0) as UUID,
            sortValue = (get(1) as Number).toLong(),
        )

    private fun addBaseConditions(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Link>,
        sourceCompanyUserId: UUID?,
        tag: String?,
        predicates: MutableList<Predicate>,
    ) {
        sourceCompanyUserId?.let { addSourceCompanyUserIdCondition(cb, query, root, it, predicates) }
        tag?.let { addTagCondition(cb, query, root, it, predicates) }
    }

    private fun addSourceCompanyUserIdCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Link>,
        sourceCompanyUserId: UUID,
        predicates: MutableList<Predicate>,
    ) {
        val subquery = query.subquery(Long::class.java)
        val userLinkRoot = subquery.from(UserLink::class.java)

        subquery.select(cb.literal(1L))
        subquery.where(
            cb.equal(userLinkRoot.get(UserLink_.link).get(EntityBase_.id), root.get(EntityBase_.id)),
            cb.equal(userLinkRoot.get(UserLink_.user).get(EntityBase_.id), sourceCompanyUserId),
        )
        predicates.add(cb.exists(subquery))
    }

    private fun addTagCondition(
        cb: CriteriaBuilder,
        query: CommonAbstractCriteria,
        root: Root<Link>,
        tag: String,
        predicates: MutableList<Predicate>,
    ) {
        val subquery = query.subquery(Long::class.java)
        val tagLinkRoot = subquery.from(Link::class.java)
        val tagJoin = tagLinkRoot.join(Link_.tags, JoinType.INNER)

        subquery.select(cb.literal(1L))
        subquery.where(
            cb.equal(tagLinkRoot.get(EntityBase_.id), root.get(EntityBase_.id)),
            cb.equal(tagJoin.get(Tag_.name), tag),
        )
        predicates.add(cb.exists(subquery))
    }

    private fun addStatsJoinCondition(
        cb: CriteriaBuilder,
        root: Root<Link>,
        statsRoot: Root<LinkDailyStats>,
        period: LinkPeriod,
        predicates: MutableList<Predicate>,
    ) {
        predicates.add(cb.equal(statsRoot.get(LinkDailyStats_.link).get(EntityBase_.id), root.get(EntityBase_.id)))
        period.days?.let { days ->
            predicates.add(cb.greaterThanOrEqualTo(statsRoot.get(LinkDailyStats_.statDate), statsCutoffDate(days)))
        }
    }

    /**
     * 발행일 정렬 순서: 발행일 있는 링크(발행일 DESC) → 발행일 없는 링크, 동일 시 id DESC
     */
    private fun publishedSortOrders(
        cb: CriteriaBuilder,
        root: Root<Link>,
    ): List<Order> {
        val nullPublishedLast =
            cb.selectCase<Int>()
                .`when`(cb.isNull(root.get(Link_.publishedAt)), 1)
                .otherwise(0)
        return listOf(
            cb.asc(nullPublishedLast),
            cb.desc(root.get(Link_.publishedAt)),
            cb.desc(root.get(EntityBase_.id)),
        )
    }

    /**
     * 발행일 정렬 커서 조건
     *
     * 커서의 발행일이 있으면: (발행일 < cursor) OR (발행일 = cursor AND id < cursorId) OR 발행일 없음
     * 커서의 발행일이 없으면(발행일 없는 구간 진행 중): 발행일 없음 AND id < cursorId
     */
    private fun buildPublishedCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Link>,
        cursor: LinkCursorV1,
    ): Predicate {
        val publishedAtPath = root.get(Link_.publishedAt)
        val idPath = root.get(EntityBase_.id)
        val cursorPublishedAt = cursor.sortInstant

        if (cursorPublishedAt == null) {
            return cb.and(cb.isNull(publishedAtPath), cb.lessThan(idPath, cursor.id))
        }

        val publishedLess =
            cb.and(
                cb.isNotNull(publishedAtPath),
                cb.lessThan(publishedAtPath, cursorPublishedAt),
            )
        val publishedEqualIdLess =
            cb.and(
                cb.equal(publishedAtPath, cursorPublishedAt),
                cb.lessThan(idPath, cursor.id),
            )
        return cb.or(publishedLess, publishedEqualIdLess, cb.isNull(publishedAtPath))
    }

    /**
     * count 기반 정렬 커서 조건 (좋아요/저장 기간 집계 합)
     *
     * 집계 합이 동일한 링크 구분을 위해 createdAt, id를 보조 키로 사용합니다.
     * 조건: (합 < cursor) OR (합 = cursor AND createdAt < cursor) OR (합 = cursor AND createdAt = cursor AND id < cursorId)
     */
    private fun buildCountCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Link>,
        cursor: LinkCursorV1,
        sortExpression: Expression<Long>,
    ): Predicate {
        val createdAtPath = root.get<Instant>(EntityBase_.createdAt)
        val idPath = root.get<UUID>(EntityBase_.id)
        val cursorCreatedAt =
            requireNotNull(cursor.sortInstant) { "집계 정렬 커서에는 createdAt이 필요합니다" }

        val countLess = cb.lessThan(sortExpression, cursor.sortValue)
        val countEqualCreatedAtLess =
            cb.and(
                cb.equal(sortExpression, cursor.sortValue),
                cb.lessThan(createdAtPath, cursorCreatedAt),
            )
        val countEqualCreatedAtEqualIdLess =
            cb.and(
                cb.equal(sortExpression, cursor.sortValue),
                cb.equal(createdAtPath, cursorCreatedAt),
                cb.lessThan(idPath, cursor.id),
            )
        return cb.or(countLess, countEqualCreatedAtLess, countEqualCreatedAtEqualIdLess)
    }

    private fun countSortOrders(
        cb: CriteriaBuilder,
        root: Root<Link>,
        sortExpression: Expression<Long>,
    ): List<Order> =
        listOf(
            cb.desc(sortExpression),
            cb.desc(root.get(EntityBase_.createdAt)),
            cb.desc(root.get(EntityBase_.id)),
        )

    private fun dailyStatsSumExpression(
        cb: CriteriaBuilder,
        statsRoot: Root<LinkDailyStats>,
        sortType: LinkSortType,
    ): Expression<Long> = cb.coalesce(cb.sum(dailyStatsCountExpression(statsRoot, sortType)), 0L)

    private fun dailyStatsCountExpression(
        statsRoot: Root<LinkDailyStats>,
        sortType: LinkSortType,
    ): Path<Long> =
        when (sortType) {
            LinkSortType.LIKE -> statsRoot.get(LinkDailyStats_.likeCount)
            LinkSortType.SAVE -> statsRoot.get(LinkDailyStats_.saveCount)
            LinkSortType.PUBLISHED -> throw IllegalStateException("PUBLISHED는 일별 집계 정렬을 사용하지 않습니다")
        }

    private fun statsCutoffDate(days: Int): LocalDate = LocalDate.now(ZoneOffset.UTC).minusDays(days.toLong())
}
