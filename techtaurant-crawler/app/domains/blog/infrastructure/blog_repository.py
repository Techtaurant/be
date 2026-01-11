"""
Blog Repository

블로그 데이터베이스 작업을 처리하는 레포지토리
"""

from typing import Sequence
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domains.blog.entity.blog import Blog


class BlogRepository:
    """블로그 레포지토리"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, blog: Blog) -> Blog:
        """블로그 생성"""
        self.session.add(blog)
        await self.session.flush()
        return blog

    async def find_by_id(self, id: UUID) -> Blog | None:
        """ID로 블로그 조회"""
        result = await self.session.execute(select(Blog).where(Blog.id == id))
        return result.scalar_one_or_none()

    async def find_by_name(self, name: str) -> Blog | None:
        """이름으로 블로그 조회 (삭제되지 않은 것만)"""
        result = await self.session.execute(
            select(Blog).where(Blog.name == name, Blog.deleted_at.is_(None))
        )
        return result.scalar_one_or_none()

    async def find_all(self) -> Sequence[Blog]:
        """모든 블로그 조회 (삭제되지 않은 것만)"""
        result = await self.session.execute(select(Blog).where(Blog.deleted_at.is_(None)))
        return result.scalars().all()
