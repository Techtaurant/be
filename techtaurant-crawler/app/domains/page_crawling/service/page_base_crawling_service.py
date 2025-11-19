from app.domains.page_crawling.dto.link_crawling_response import LinkCrawlingResponse
from app.domains.page_crawling.dto.page_base_link_crawling_request import (
    PageBaseLinkCrawlingRequest,
)
from app.domains.page_crawling.service.page_base_crawler import PageBaseCrawler


class PageBaseLinkCrawlingService:
    async def process(self, command: PageBaseLinkCrawlingRequest) -> list[LinkCrawlingResponse]:
        crawler = PageBaseCrawler(command)

        return await crawler.fetch_content()
