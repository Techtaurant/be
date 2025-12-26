"""
Blog 엔티티

블로그 정보를 저장하는 모델
"""

from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID, uuid4

from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.configs.database import Base

if TYPE_CHECKING:
    from app.domains.article_link.entity.article_link import ArticleLink


class Blog(Base):
    """
    블로그 정보

    크롤링 대상 블로그의 기본 정보를 저장합니다.
    """

    __tablename__ = "blogs"

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    name: Mapped[str] = mapped_column(String(100), nullable=False, unique=True, index=True)
    display_name: Mapped[str | None] = mapped_column(String(200), nullable=True)
    icon_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    base_url: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, index=True)

    # Relationship
    article_links: Mapped[list[ArticleLink]] = relationship(
        "ArticleLink", back_populates="blog", cascade="all, delete-orphan"
    )
