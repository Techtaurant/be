from urllib.parse import urljoin

from bs4 import BeautifulSoup

from app.common.utils.browser_page_pool import BrowserPagePool
from app.domains.page_crawling.dto.link_crawling_response import LinkCrawlingResponse
from app.domains.page_crawling.dto.page_base_link_crawling_request import (
    PageBaseLinkCrawlingRequest,
)


class PageBaseCrawler:
    def __init__(
        self,
        crawling_command: PageBaseLinkCrawlingRequest,
        browser_pool: BrowserPagePool,
    ):
        self.base_url = crawling_command.base_url
        self.start_page = crawling_command.start_page
        self.article_pattern = crawling_command.article_pattern
        self.title_selector = crawling_command.title_selector
        self.browser_pool = browser_pool

    def __extract_articles(self, soup: BeautifulSoup) -> list[LinkCrawlingResponse]:
        """게시글 링크와 제목을 추출"""
        articles: list[LinkCrawlingResponse] = []
        a_tags = soup.find_all("a", href=True)

        for a_tag in a_tags:
            href = a_tag.get("href")

            # article 링크만 필터링
            if not href or self.article_pattern not in href:
                continue

            # 제목 추출 (선택자로 찾기 시도)
            title_element = a_tag.select_one(self.title_selector)
            title = (
                title_element.get_text(strip=True) if title_element else a_tag.get_text(strip=True)
            )

            # 절대 URL 변환
            absolute_url = urljoin(self.base_url, href)

            articles.append(LinkCrawlingResponse(url=absolute_url, title=title))

        return articles

    async def __crawl_blog(self, page: int) -> list[LinkCrawlingResponse]:
        """블로그 크롤링"""

        # 페이지 URL 구성
        request_url = f"{self.base_url}?page={page}"

        # 브라우저 풀에서 페이지 획득
        browser_page = await self.browser_pool.acquire()
        try:
            await browser_page.goto(request_url, wait_until="networkidle")
            html_content = await browser_page.content()
        finally:
            # 사용한 페이지 반환
            await self.browser_pool.release(browser_page)

        soup = BeautifulSoup(html_content, "html.parser")

        return self.__extract_articles(soup)

    async def fetch_content(self) -> list[LinkCrawlingResponse]:
        total_results: list[LinkCrawlingResponse] = []
        """페이지 크롤링 수행"""
        while True:
            results = await self.__crawl_blog(self.start_page)
            total_results.extend(results)

            if len(results) == 0:
                break

            self.start_page += 1
        return total_results
