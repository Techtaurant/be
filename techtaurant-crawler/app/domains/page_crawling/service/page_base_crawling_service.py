from app.common.utils.browser_page_pool import BrowserPagePool
from app.domains.page_crawling.dto.link_crawling_response import LinkCrawlingResponse
from app.domains.page_crawling.dto.page_base_link_crawling_request import (
    PageBaseLinkCrawlingRequest,
)
from app.domains.page_crawling.service.page_base_crawler import PageBaseCrawler


class PageBaseLinkCrawlingService:
    def __init__(self, browser_pool: BrowserPagePool):
        self.browser_pool = browser_pool

    async def validate(self, command: PageBaseLinkCrawlingRequest) -> bool:
        crawler = PageBaseCrawler(command, self.browser_pool)

        return await crawler.check_one_page()
