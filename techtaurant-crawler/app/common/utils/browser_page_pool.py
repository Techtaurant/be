import asyncio
import logging
from dataclasses import dataclass, field
from typing import Literal

from fastapi import Request
from playwright._impl._errors import TargetClosedError
from playwright.async_api import (
    Browser,
    BrowserContext,
    BrowserType,
    Page,
    Playwright,
    async_playwright,
)

logger = logging.getLogger(__name__)


@dataclass
class BrowserPagePool:
    max_pages: int
    browser_type: Literal["chromium", "firefox", "webkit"] = "chromium"
    launch_options: dict = field(default_factory=dict)
    context_options: dict = field(default_factory=dict)

    _playwright: Playwright | None = field(default=None, init=False, repr=False)
    _browser: Browser | None = field(default=None, init=False, repr=False)
    _context: BrowserContext | None = field(default=None, init=False, repr=False)
    _pages: list[Page] = field(default_factory=list, init=False, repr=False)
    _lock: asyncio.Lock = field(default_factory=asyncio.Lock, init=False, repr=False)
    _page_available: asyncio.Condition = field(init=False, repr=False)
    _initialized: bool = field(default=False, init=False, repr=False)

    def __post_init__(self):
        self._page_available = asyncio.Condition(lock=self._lock)

    async def _initialize(self):
        if self._initialized:
            return

        self._playwright = await async_playwright().__aenter__()
        browser_launcher: BrowserType = getattr(self._playwright, self.browser_type)
        self._browser = await browser_launcher.launch(**self.launch_options)
        self._context = await self._browser.new_context(**self.context_options)

        for _ in range(self.max_pages):
            page = await self._context.new_page()
            self._pages.append(page)

        self._initialized = True

    async def __aenter__(self) -> "BrowserPagePool":
        await self._initialize()
        return self

    async def __aexit__(self, _exc_type=None, _exc_val=None, _exc_tb=None):
        async with self._lock:
            if self._pages:
                for page in self._pages:
                    if page and not page.is_closed():
                        try:
                            await page.close()
                        except (TargetClosedError, Exception):
                            pass
                self._pages.clear()

            if self._context:
                try:
                    await self._context.close()
                except (TargetClosedError, Exception):
                    pass
                self._context = None

            if self._browser:
                try:
                    await self._browser.close()
                except (TargetClosedError, Exception):
                    pass
                self._browser = None

            if self._playwright:
                try:
                    await self._playwright.stop()
                except Exception as e:
                    logger.error(f"Playwright 종료 중 오류: {e}")
                self._playwright = None

        self._initialized = False

    async def acquire(self, timeout: float | None = None) -> Page:
        if not self._initialized:
            raise RuntimeError("BrowserPagePool이 초기화되지 않았습니다")

        async with self._lock:
            while not self._pages:
                if timeout is None:
                    await self._page_available.wait()
                else:
                    try:
                        await asyncio.wait_for(self._page_available.wait(), timeout=timeout)
                    except TimeoutError:
                        raise TimeoutError("페이지 획득 타임아웃")

            return self._pages.pop()

    async def release(self, page: Page):
        if not self._initialized:
            if page and not page.is_closed():
                await page.close()
            return

        async with self._lock:
            if page and not page.is_closed():
                self._pages.append(page)
                self._page_available.notify()


def create_browser_pool() -> BrowserPagePool:
    """
    브라우저 풀 인스턴스 생성 - 팩토리 함수

    Returns:
        BrowserPagePool: 설정이 적용된 브라우저 풀 인스턴스
    """
    return BrowserPagePool(
        max_pages=1,
        browser_type="chromium",
        launch_options={
            "headless": True,
            "args": [
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-web-security",
                "--memory-pressure-off",
                "--max_old_space_size=4096",
            ],
        },
        context_options={},
    )


def get_browser_pool(request: Request) -> BrowserPagePool:
    """
    브라우저 풀 인스턴스 반환 - FastAPI 의존성 주입용

    Args:
        request: FastAPI Request 객체

    Returns:
        BrowserPagePool: app.state에 저장된 브라우저 풀 인스턴스

    Raises:
        RuntimeError: 브라우저 풀이 초기화되지 않은 경우
    """
    if not hasattr(request.app.state, "browser_pool"):
        raise RuntimeError("브라우저 풀이 초기화되지 않았습니다")
    return request.app.state.browser_pool
