package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.common.policy.TemporaryContentRetention
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 보관 기간이 지난 임시 콘텐츠를 주기적으로 회수한다.
 *
 * 만료된 임시저장과 그 첨부, 그리고 어느 대상에도 소유가 기록되지 않은 만료 첨부를 한 번의 실행에서
 * 함께 정리한다. 두 대상은 정리 주체가 서로 달라서 따로 두면 한쪽만 사라지는 구간이 생긴다.
 * 임시저장 목록 조회도 같은 정리를 수행하지만 목록을 열지 않는 사용자의 임시저장은 그 경로에 잡히지
 * 않으므로, 보관 기간의 상한은 이 배치가 보장한다.
 *
 * 삭제가 멱등이라 여러 인스턴스가 동시에 실행해도 안전하다.
 */
@Component
class TemporaryContentCleanupScheduler(
    private val expiredDraftCleanupService: ExpiredDraftCleanupService,
    private val attachmentService: AttachmentService,
    private val temporaryContentRetention: TemporaryContentRetention,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CLEANUP_INTERVAL_MILLIS = 86_400_000L

        // 한 번에 지우는 양을 제한해 이미 쌓인 물량이 많아도 트랜잭션과 S3 삭제 요청이 한꺼번에 몰리지 않게 한다.
        // 임시저장은 건당 첨부 조회와 S3 삭제가 따라붙어 첨부 단독 정리보다 무거우므로 상한을 낮게 잡는다.
        private const val MAX_DRAFT_DELETE_COUNT_PER_RUN = 100
        private const val MAX_ATTACHMENT_DELETE_COUNT_PER_RUN = 500
    }

    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MILLIS)
    fun deleteExpiredTemporaryContent() {
        val expirationThreshold = temporaryContentRetention.expirationThreshold()

        // 임시저장을 먼저 지워야 그 첨부가 여기서 회수되고, 남은 미확정 첨부만 다음 단계가 훑는다.
        val deletedDraftCount = expiredDraftCleanupService.deleteExpiredDrafts(MAX_DRAFT_DELETE_COUNT_PER_RUN, null)
        val deletedAttachmentCount =
            attachmentService.deleteExpiredTmpAttachments(expirationThreshold, MAX_ATTACHMENT_DELETE_COUNT_PER_RUN)

        if (deletedDraftCount > 0 || deletedAttachmentCount > 0) {
            log.info(
                "Deleted {} expired drafts and {} expired tmp attachments older than {}",
                deletedDraftCount,
                deletedAttachmentCount,
                expirationThreshold,
            )
        }
    }
}
