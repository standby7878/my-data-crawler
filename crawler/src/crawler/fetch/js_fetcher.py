from __future__ import annotations

import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class JsFetchResult:
    url: str
    final_url: str | None
    status_code: int | None
    content: str | None
    error: str | None


async def fetch_with_playwright(url: str, timeout_ms: int = 20000) -> JsFetchResult:
    try:
        from playwright.async_api import async_playwright
    except Exception as exc:
        return JsFetchResult(url=url, final_url=None, status_code=None, content=None, error=str(exc))

    try:
        async with async_playwright() as playwright:
            browser = await playwright.chromium.launch()
            page = await browser.new_page()
            response = await page.goto(url, wait_until="domcontentloaded", timeout=timeout_ms)
            await page.wait_for_timeout(1000)
            content = await page.content()
            final_url = page.url
            status = response.status if response else None
            await browser.close()
            return JsFetchResult(url=url, final_url=final_url, status_code=status, content=content, error=None)
    except Exception as exc:
        logger.warning("Playwright fetch failed for %s: %s", url, exc)
        return JsFetchResult(url=url, final_url=None, status_code=None, content=None, error=str(exc))
