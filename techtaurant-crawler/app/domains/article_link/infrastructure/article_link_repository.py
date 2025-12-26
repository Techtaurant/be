"""
ArticleLink Repository

게시글 링크 데이터베이스 작업을 처리하는 레포지토리
"""

from collections.abc import Sequence
from datetime import datetime
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domains.article_link.entity.article_link import ArticleLink, ArticleLinkType


class ArticleLinkRepository:
    """게시글 링크 레포지토리"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, article_link: ArticleLink) -> ArticleLink:
        """게시글 링크 생성"""
        self.session.add(article_link)
        await self.session.flush()
        return article_link

    async def create_bulk(self, article_links: list[ArticleLink]) -> list[ArticleLink]:
        """게시글 링크 일괄 생성"""
        self.session.add_all(article_links)
        await self.session.flush()
        return article_links

    async def find_by_id(self, id: UUID) -> ArticleLink | None:
        """ID로 게시글 링크 조회"""
        result = await self.session.execute(select(ArticleLink).where(ArticleLink.id == id))
        return result.scalar_one_or_none()

    async def find_by_blog_id(self, blog_id: UUID) -> Sequence[ArticleLink]:
        """블로그 ID로 게시글 링크 목록 조회 (삭제되지 않은 것만)"""
        result = await self.session.execute(
            select(ArticleLink).where(
                ArticleLink.blog_id == blog_id, ArticleLink.deleted_at.is_(None)
            )
        )
        return result.scalars().all()

    async def find_by_type(self, type: ArticleLinkType) -> Sequence[ArticleLink]:
        """타입으로 게시글 링크 목록 조회 (삭제되지 않은 것만)"""
        result = await self.session.execute(
            select(ArticleLink).where(ArticleLink.type == type, ArticleLink.deleted_at.is_(None))
        )
        return result.scalars().all()

    async def soft_delete(self, id: UUID) -> ArticleLink | None:
        """게시글 링크 소프트 삭제"""
        article_link = await self.find_by_id(id)
        if article_link:
            article_link.deleted_at = datetime.utcnow()
            await self.session.flush()
        return article_link
