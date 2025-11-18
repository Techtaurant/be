from pydantic import BaseModel, ConfigDict, Field


class PageBaseLinkCrawlingRequest(BaseModel):
    """
    페이지 기반 링크 크롤링 요청 DTO

    블로그나 게시판의 페이지를 순회하며 게시글 링크를 크롤링하기 위한 설정 정보를 담습니다.
    """

    blog_name: str = Field(
        ...,
        description="크롤링할 블로그 이름 (식별자)",
        min_length=1,
        max_length=100,
        examples=["techtaurant", "example_blog"],
    )

    base_url: str = Field(
        ...,
        description="크롤링 기본 URL (쿼리 파라미터 없이). 예: https://example.com/posts",
        min_length=1,
        pattern=r"^https?://",
        examples=["https://example.com/posts", "https://blog.example.com"],
    )

    start_page: int = Field(
        default=0,
        description="크롤링을 시작할 페이지 번호",
        ge=0,
        examples=[0, 1],
    )

    article_pattern: str = Field(
        default="",
        description="게시글 URL에 포함되는 고유한 패턴 (필터링용)",
        max_length=200,
        examples=["/articles/", "/posts/", "/blog/"],
    )

    title_selector: str = Field(
        default="",
        description="게시글 제목을 추출하기 위한 CSS 선택자",
        max_length=200,
        examples=[".title", "h2.post-title", "span.article-title"],
    )

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "blog_name": "techtaurant",
                "base_url": "https://techtaurant.com/posts",
                "start_page": 1,
                "article_pattern": "/articles/",
                "title_selector": "h2.post-title",
            }
        }
    )
