from pydantic import BaseModel, Field


class LinkCrawlingResponse(BaseModel):
    """
    크롤링된 링크 정보 응답 DTO

    각 게시글의 URL과 제목 정보를 담습니다.
    """

    url: str = Field(
        ...,
        description="크롤링된 게시글의 URL",
        examples=["https://techtaurant.com/articles/123"],
    )

    title: str = Field(
        ...,
        description="게시글 제목",
        examples=["FastAPI로 크롤러 만들기"],
    )
