"""
BatchLog Repository

배치 로그 데이터베이스 작업을 처리하는 레포지토리
"""

from collections.abc import Sequence
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domains.batch_log.entity.batch_log import BatchLog, BatchStatus


class BatchLogRepository:
    """배치 로그 레포지토리"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, batch_log: BatchLog) -> BatchLog:
        """배치 로그 생성"""
        self.session.add(batch_log)
        await self.session.flush()
        return batch_log

    async def find_by_id(self, id: UUID) -> BatchLog | None:
        """ID로 배치 로그 조회"""
        result = await self.session.execute(select(BatchLog).where(BatchLog.id == id))
        return result.scalar_one_or_none()

    async def find_by_batch_name(self, batch_name: str) -> Sequence[BatchLog]:
        """배치 이름으로 로그 목록 조회"""
        result = await self.session.execute(
            select(BatchLog).where(BatchLog.batch_name == batch_name)
        )
        return result.scalars().all()

    async def find_by_status(self, status: BatchStatus) -> Sequence[BatchLog]:
        """상태로 배치 로그 목록 조회"""
        result = await self.session.execute(select(BatchLog).where(BatchLog.status == status))
        return result.scalars().all()

    async def update(self, batch_log: BatchLog) -> BatchLog:
        """배치 로그 업데이트"""
        await self.session.flush()
        return batch_log
