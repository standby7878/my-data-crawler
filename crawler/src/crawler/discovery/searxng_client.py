import asyncio
from dataclasses import dataclass
from typing import Iterable, List, Optional
import httpx
import logging

logger = logging.getLogger(__name__)


@dataclass
class SearchResult:
    url: str
    title: str
    content: str
    engine: Optional[str] = None


class SearXngClient:
    def __init__(self, base_url: str, timeout: int = 30, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self._client: Optional[httpx.AsyncClient] = None
        self._transport = transport

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(self.timeout),
                follow_redirects=True,
                transport=self._transport,
                headers={
                    "User-Agent": "JobCrawler/0.1 SearXNG-Client",
                    "Accept": "application/json",
                    "Accept-Language": "en-US,en;q=0.5",
                    "Accept-Encoding": "gzip, deflate",
                    "Connection": "keep-alive",
                    "X-Forwarded-For": "127.0.0.1",
                    "X-Real-IP": "127.0.0.1",
                    "Referer": self.base_url,
                },
            )
        return self._client

    async def search(
        self,
        query: str,
        *,
        categories: Optional[Iterable[str]] = None,
        engines: Optional[Iterable[str]] = None,
        lang: str = "fi",
        max_results: int = 30,
    ) -> List[SearchResult]:
        results: List[SearchResult] = []
        page_no = 1
        max_pages = 10
        while len(results) < max_results and page_no <= max_pages:
            page = await self._fetch_page(query, page_no, categories, engines, lang)
            if not page:
                break
            results.extend(page)
            if len(page) < 20:
                break
            page_no += 1
            await asyncio.sleep(0.5)
        return results[:max_results]

    async def _fetch_page(
        self,
        query: str,
        page_no: int,
        categories: Optional[Iterable[str]],
        engines: Optional[Iterable[str]],
        lang: str,
    ) -> List[SearchResult]:
        params = {
            "q": query,
            "format": "json",
            "lang": lang,
            "pageno": str(page_no),
        }
        if categories:
            params["categories"] = ",".join(categories)
        if engines:
            params["engines"] = ",".join(engines)

        client = await self._get_client()
        url = f"{self.base_url}/search"
        logger.info("SearXNG query page %s: %s", page_no, query)
        response = await client.get(url, params=params)
        if response.status_code == 429:
            logger.warning("SearXNG rate limited, backing off")
            await asyncio.sleep(2)
            response = await client.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        items = data.get("results", [])
        parsed: List[SearchResult] = []
        for item in items:
            parsed.append(
                SearchResult(
                    url=item.get("url", ""),
                    title=item.get("title", ""),
                    content=item.get("content", ""),
                    engine=item.get("engine"),
                )
            )
        return parsed

    async def close(self) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None
