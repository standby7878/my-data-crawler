import httpx
import pytest

from crawler.fetch.http_fetcher import fetch_urls


@pytest.mark.asyncio
async def test_fetch_urls_returns_content():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, text=f"content for {request.url}")

    transport = httpx.MockTransport(handler)
    results = await fetch_urls(["https://example.com/a", "https://example.com/b"], transport=transport)

    assert len(results) == 2
    assert "content" in results[0].content
