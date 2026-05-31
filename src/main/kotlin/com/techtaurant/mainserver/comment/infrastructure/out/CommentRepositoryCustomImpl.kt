package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.dto.CommentCursor
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.entity.Comment_
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.base.EntityBase_
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 댓글 동적 쿼리 구현체
 *
 * JPA Criteria API와 Metamodel을 사용하여 정렬, 커서 기반 페이지네이션을 지원
 */
@Repository
class CommentRepositoryCustomImpl : CommentRepositoryCustom {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findCommentsByIdsIncludingDeleted(commentIds: List<UUID>): List<Comment> {
        if (commentIds.isEmpty()) {
            return emptyList()
        }

        return entityManager.createQuery(
            """
            SELECT c
            FROM Comment c
            LEFT JOIN FETCH c.author
            JOIN FETCH c.post
            WHERE c.id IN :commentIds
            """.trimIndent(),
            Comment::class.java,
        )
            .setParameter("commentIds", commentIds)
            .resultList
    }

    /**
     * 삭제된 댓글을 포함해 부모 댓글 목록을 조회합니다. (depth=0)
     *
     * N+1 방지를 위해 author, post를 fetch join하며 커서 기반 페이지네이션으로 조회
     */
    override fun findParentCommentsIncludingDeletedWithConditions(
        postId: UUID,
        cursor: CommentCursor?,
        size: Int,
        sortType: CommentSortType,
    ): List<Comment> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Comment::class.java)
        val root = cq.from(Comment::class.java)

        root.fetch<Comment, User>(Comment_.AUTHOR, JoinType.LEFT)
        root.fetch<Comment, Post>(Comment_.POST, JoinType.LEFT)

        val predicates = mutableListOf<Predicate>()

        predicates.add(cb.equal(root.get(Comment_.post).get(EntityBase_.id), postId))
        predicates.add(cb.equal(root.get(Comment_.depth), 0))

        addCursorCondition(cb, root, cursor, sortType, predicates)

        cq.where(*predicates.toTypedArray())
        applyOrdering(cb, cq, root, sortType)

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .resultList
    }

    /**
     * 삭제된 댓글을 포함해 대댓글 목록을 조회합니다. (depth=1)
     *
     * N+1 방지를 위해 author, post를 fetch join하며 커서 기반 페이지네이션으로 조회
     */
    override fun findRepliesIncludingDeletedWithConditions(
        parentId: UUID,
        cursor: CommentCursor?,
        size: Int,
        sortType: CommentSortType,
    ): List<Comment> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Comment::class.java)
        val root = cq.from(Comment::class.java)

        root.fetch<Comment, User>(Comment_.AUTHOR, JoinType.LEFT)
        root.fetch<Comment, Post>(Comment_.POST, JoinType.LEFT)

        val predicates = mutableListOf<Predicate>()

        predicates.add(cb.equal(root.get(Comment_.parent).get(EntityBase_.id), parentId))
        predicates.add(cb.equal(root.get(Comment_.depth), 1))

        addCursorCondition(cb, root, cursor, sortType, predicates)

        cq.where(*predicates.toTypedArray())
        applyOrdering(cb, cq, root, sortType)

        return entityManager.createQuery(cq)
            .setMaxResults(size)
            .resultList
    }

    /**
     * 정렬 타입에 따른 커서 조건 추가
     *
     * 최신순은 createdAt + id 기준, 그 외(추천/답글순)는 해당 count + createdAt + id 기준
     */
    private fun addCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Comment>,
        cursor: CommentCursor?,
        sortType: CommentSortType,
        predicates: MutableList<Predicate>,
    ) {
        cursor ?: return

        val cursorPredicate =
            when (sortType) {
                CommentSortType.LATEST -> buildLatestCursorCondition(cb, root, cursor)
                else -> buildCountCursorCondition(cb, root, cursor, sortType)
            }
        predicates.add(cursorPredicate)
    }

    /**
     * 최신순 커서 조건 생성
     *
     * 동일 시간대 댓글 구분을 위해 id를 보조 키로 사용
     * 조건: (createdAt < cursor) OR (createdAt = cursor AND id < cursorId)
     */
    private fun buildLatestCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Comment>,
        cursor: CommentCursor,
    ): Predicate {
        val createdAtLess = cb.lessThan(root.get(EntityBase_.createdAt), cursor.createdAt)
        val createdAtEqualAndIdLess =
            cb.and(
                cb.equal(root.get(EntityBase_.createdAt), cursor.createdAt),
                cb.lessThan(root.get(EntityBase_.id), cursor.id),
            )
        return cb.or(createdAtLess, createdAtEqualAndIdLess)
    }

    /**
     * count 기반 정렬의 커서 조건 생성 (추천수, 답글수)
     *
     * count가 동일한 댓글 구분을 위해 createdAt, id를 보조 키로 사용
     * 조건: (count < cursor) OR (count = cursor AND createdAt < cursor) OR (count = cursor AND createdAt = cursor AND id < cursorId)
     */
    private fun buildCountCursorCondition(
        cb: CriteriaBuilder,
        root: Root<Comment>,
        cursor: CommentCursor,
        sortType: CommentSortType,
    ): Predicate {
        val sortAttribute =
            when (sortType) {
                CommentSortType.LIKE -> Comment_.likeCount
                CommentSortType.REPLY -> Comment_.replyCount
                else -> Comment_.likeCount
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
     * 모든 정렬은 DESC이며 동점자 처리를 위해 createdAt, id를 보조 정렬 키로 추가
     */
    private fun applyOrdering(
        cb: CriteriaBuilder,
        cq: CriteriaQuery<Comment>,
        root: Root<Comment>,
        sortType: CommentSortType,
    ) {
        val orders =
            when (sortType) {
                CommentSortType.LATEST ->
                    listOf(
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                CommentSortType.LIKE ->
                    listOf(
                        cb.desc(root.get(Comment_.likeCount)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
                CommentSortType.REPLY ->
                    listOf(
                        cb.desc(root.get(Comment_.replyCount)),
                        cb.desc(root.get(EntityBase_.createdAt)),
                        cb.desc(root.get(EntityBase_.id)),
                    )
            }
        cq.orderBy(orders)
    }
}
