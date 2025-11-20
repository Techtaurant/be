from fastapi import APIRouter, Depends, status

from app.common.base_response import BaseResponse, success_response
from app.common.constants import API_V1
from app.common.utils.browser_page_pool import BrowserPagePool, get_browser_pool
from app.domains.page_crawling.dto.page_base_link_crawling_request import (
    PageBaseLinkCrawlingRequest,
)
from app.domains.page_crawling.service.page_base_link_crawling_service import (
    PageBaseLinkCrawlingService,
)

page_base_link_crawling_router = APIRouter(
    prefix=f"{API_V1}/page-base-link-crawling",
    tags=["페이지 기반 링크 크롤링"],
)


@page_base_link_crawling_router.post(
    "/validations",
    summary="크롤링 페이지 유효성 검증",
    description="""
    크롤링 페이지 등록 전 해당 설정으로 크롤링이 가능한지 검증합니다.

    ## 동작 방식
    1. `base_url`과 `start_page`를 조합하여 페이지 URL 생성
    2. 각 페이지에서 `article_pattern`에 매칭되는 링크가 존재하는지 확인
    3. 크롤링 가능 여부를 boolean으로 반환

    ## 사용 목적
    - 크롤링 페이지 최초 등록 시 설정값이 올바른지 검증
    - 잘못된 설정으로 인한 크롤링 실패 방지

    ## 주의사항
    - 대상 사이트의 robots.txt를 준수하세요
    - 과도한 요청은 IP 차단의 원인이 될 수 있습니다
    """,
    response_description="크롤링 가능 여부",
    status_code=status.HTTP_200_OK,
    responses={
        200: {
            "description": "검증 성공",
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "status": "1001",
                        "message": "요청이 성공했습니다",
                        "data": True,
                        "timestamp": "2025-11-20T14:21:10.745039Z",
                    }
                }
            },
        },
    },
)
async def validate_crawling_page(
    command: PageBaseLinkCrawlingRequest,
    browser_pool: BrowserPagePool = Depends(get_browser_pool),
) -> BaseResponse[bool]:
    """
    크롤링 페이지 유효성 검증 엔드포인트

    Args:
        command: 크롤링 설정 정보 (블로그명, URL, 페이지 번호, 패턴 등)
        browser_pool: 브라우저 페이지 풀 (의존성 주입)

    Returns:
        BaseResponse[bool]: 크롤링 가능 여부

    Raises:
        HTTPException: 검증 중 오류 발생 시
    """
    page_base_link_crawling_service = PageBaseLinkCrawlingService(browser_pool)
    result = await page_base_link_crawling_service.validate(command)
    return success_response(data=result)
