package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.policy.TemporaryContentRetention
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 보관 기간이 지난 임시저장 게시물을 첨부와 함께 회수한다.
 *
 * 임시저장은 첨부를 확정하지 않고 소유만 기록하므로 첨부는 tmp/ 경로에 TMP 상태로 남는다.
 * 미확정 첨부 정리 배치는 소유가 기록된 첨부를 건너뛰기 때문에, 임시저장 자체를 지우는 이 경로가
 * 함께 정리하지 않으면 그 첨부는 회수할 주체가 없다.
 *
 * 정리 배치와 임시저장 목록 조회 두 곳이 호출하므로 어느 한쪽 서비스에 두지 않는다.
 */
@Service
class ExpiredDraftCleanupService(
    private val postRepository: PostRepository,
    private val attachmentService: AttachmentService,
    private val temporaryContentRetention: TemporaryContentRetention,
) {
    /**
     * 보관 기간이 지난 임시저장을 첨부파일과 함께 삭제합니다.
     *
     * 첨부를 먼저 지워도 `posts.thumbnail_image` 외래키가 ON DELETE SET NULL이라 참조가 남지 않습니다.
     *
     * 호출자의 트랜잭션에 합류하지 않고 자기 트랜잭션에서 돕니다. 임시저장 목록 조회가 이 정리를 함께
     * 수행하는데, 정리 실패가 조회 트랜잭션을 rollback-only로 만들면 목록 자체를 응답하지 못한다.
     *
     * @param limit 한 번에 삭제할 최대 게시물 수
     * @param authorId 작성자 ID (null이면 작성자를 가리지 않고 정리)
     * @return 삭제한 임시저장 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun deleteExpiredDrafts(
        limit: Int,
        authorId: UUID?,
    ): Int {
        val staleDrafts = postRepository.findStaleDrafts(temporaryContentRetention.expirationThreshold(), limit, authorId)
        if (staleDrafts.isEmpty()) return 0

        attachmentService.deleteAttachmentsByReferenceIds(staleDrafts.mapNotNull { it.id }, AttachmentReferenceType.POST)
        postRepository.deleteAll(staleDrafts)

        return staleDrafts.size
    }
}
