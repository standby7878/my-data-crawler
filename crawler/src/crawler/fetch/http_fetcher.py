import asyncio
from dataclasses import dataclass
from typing import Iterable, List, Optional
import httpx
import logging

from crawler.fetch.robots import RobotsManager

logger = logging.getLogger(__name__)


@dataclass
class FetchResult:
    url: str
    status_code: int
    content: str


async def fetch_url(
    url: str,
    *,
    timeout: int = 30,
    transport: httpx.AsyncBaseTransport | None = None,
    user_agent: str = "Mozilla/5.0 (compatible; JobCrawler/0.1; +http://localhost)",
    robots_manager: Optional[RobotsManager] = None,
    retries: int = 2,
    enforce_robots: bool = False,
) -> FetchResult:
    if robots_manager and not await robots_manager.is_allowed(url):
        if enforce_robots:
            raise PermissionError(f"Robots disallows {url}")
        logger.debug("Robots disallowed %s but enforcement disabled", url)
    async with httpx.AsyncClient(
        timeout=httpx.Timeout(timeout),
        follow_redirects=True,
        transport=transport,
    ) as client:
        last_exc: Optional[Exception] = None
        for attempt in range(retries + 1):
            if robots_manager and not await robots_manager.is_allowed(url):
                if enforce_robots:
                    raise PermissionError(f"Robots disallows {url}")
                logger.debug("Robots disallowed %s but enforcement disabled", url)
            try:
                response = await client.get(url, headers={"User-Agent": user_agent})
                response.raise_for_status()
                return FetchResult(url=url, status_code=response.status_code, content=response.text)
            except Exception as exc:
                last_exc = exc
                if attempt < retries:
                    await asyncio.sleep(1 + attempt)
                else:
                    raise last_exc


async def fetch_urls(
    urls: Iterable[str],
    concurrency: int = 5,
    transport: httpx.AsyncBaseTransport | None = None,
    user_agent: str = "Mozilla/5.0 (compatible; JobCrawler/0.1; +http://localhost)",
    robots_manager: Optional[RobotsManager] = None,
    retries: int = 2,
    enforce_robots: bool = False,
) -> List[FetchResult]:
    semaphore = asyncio.Semaphore(concurrency)

    async def _guarded_fetch(target: str) -> FetchResult:
        async with semaphore:
            logger.info("Fetching %s", target)
            return await fetch_url(
                target,
                transport=transport,
                user_agent=user_agent,
                robots_manager=robots_manager,
                retries=retries,
                enforce_robots=enforce_robots,
            )

    tasks = [asyncio.create_task(_guarded_fetch(url)) for url in urls]
    return await asyncio.gather(*tasks)
