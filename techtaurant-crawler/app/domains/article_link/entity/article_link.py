"""
ArticleLink 엔티티

크롤링한 게시글 링크 목록을 저장하는 모델
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum as PyEnum
from typing import TYPE_CHECKING
from uuid import UUID, uuid4

from sqlalchemy import DateTime, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.configs.database import Base

if TYPE_CHECKING:
    from app.domains.blog.entity.blog import Blog


class ArticleLinkType(str, PyEnum):
    """게시글 링크 타입"""

    PAGE_BASED = "PAGE_BASED"
    NEXT_BUTTON_BASED = "NEXT_BUTTON_BASED"


class ArticleLink(Base):
    """
    게시글 링크 정보

    크롤링된 게시글 링크 목록을 저장합니다.
    """

    __tablename__ = "article_links"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    blog_id: Mapped[UUID] = mapped_column(ForeignKey("blogs.id", ondelete="CASCADE"), nullable=False, index=True)
    url: Mapped[str] = mapped_column(Text, nullable=False)
    title: Mapped[str | None] = mapped_column(String(500), nullable=True)
    type: Mapped[str] = mapped_column(String(50), nullable=False, index=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, index=True)

    # Relationship
    blog: Mapped[Blog] = relationship("Blog", back_populates="article_links")
