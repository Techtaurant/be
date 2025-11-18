from fastapi import APIRouter, status

from app.common.base_response import BaseResponse, success_response
from app.common.constants import API_V1
from app.domains.page_crawling.dto.link_crawling_response import LinkCrawlingResponse
from app.domains.page_crawling.dto.page_base_link_crawling_request import (
    PageBaseLinkCrawlingRequest,
)
from app.domains.page_crawling.service.page_base_crawler import PageBaseCrawler

page_base_link_crawling_router = APIRouter(
    prefix=f"{API_V1}/page-base-link-crawling",
    tags=["페이지 기반 링크 크롤링"],
)


@page_base_link_crawling_router.post(
    "",
    summary="페이지 기반 링크 크롤링",
    description="""
    블로그나 게시판의 페이지를 순회하며 게시글 링크와 제목을 크롤링합니다.

    ## 동작 방식
    1. `base_url`과 `start_page`를 조합하여 페이지 URL 생성
    2. 각 페이지에서 `article_pattern`에 매칭되는 링크 추출
    3. `title_selector`를 사용하여 각 게시글의 제목 추출
    4. 크롤링된 모든 링크와 제목을 리스트로 반환

    ## 사용 예시
    - Toss Tech Blog: `base_url="https://toss.tech/"`, `article_pattern="/article/"`, `title_selector="span.e1sck7qg7"`
    - Kakao Tech Blog: `base_url="https://tech.kakao.com/blog"`, `article_pattern="/posts/"`, `title_selector="h4.tit_post"`
    - 게시판: `base_url="https://example.com/board"`, `article_pattern="/view/"`

    ## 주의사항
    - 대상 사이트의 robots.txt를 준수하세요
    - 과도한 요청은 IP 차단의 원인이 될 수 있습니다
    - 적절한 요청 간격을 유지하는 것을 권장합니다
    """,
    response_description="크롤링된 게시글 링크 목록",
    status_code=status.HTTP_200_OK,
    responses={
        200: {
            "description": "크롤링 성공",
            "content": {
                "application/json": {
                    "example": {
                        "data": [
                            {
                                "url": "https://toss.tech/article/toss-frontend-chapter",
                                "title": "프론트엔드 챕터, 오늘의 실험 회고",
                            },
                            {
                                "url": "https://toss.tech/article/how-to-work-health-in-toss-core",
                                "title": "토스 코어에서 일하는 방법",
                            },
                            {
                                "url": "https://toss.tech/article/toss-server-developer-interview",
                                "title": "토스 서버 개발자가 하는 일",
                            },
                        ],
                        "message": "OK",
                        "status": 200,
                    }
                }
            },
        },
    },
)
async def crawl_page_base_links(
    command: PageBaseLinkCrawlingRequest,
) -> BaseResponse[list[LinkCrawlingResponse]]:
    """
    페이지 기반 링크 크롤링 엔드포인트

    Args:
        command: 크롤링 설정 정보 (블로그명, URL, 페이지 번호, 패턴 등)

    Returns:
        BaseResponse[list[LinkCrawlingResponse]]: 크롤링된 게시글 링크 목록

    Raises:
        HTTPException: 크롤링 중 오류 발생 시
    """
    crawler = PageBaseCrawler(crawling_command=command)
    result = await crawler.fetch_content()
    return success_response(data=result)
