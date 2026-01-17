import json
import httpx
import pytest

from crawler.discovery.searxng_client import SearXngClient


@pytest.mark.asyncio
async def test_searxng_client_parses_results():
    def handler(request: httpx.Request) -> httpx.Response:
        payload = {
            "results": [
                {"url": "https://example.com/1", "title": "Job 1", "content": "A", "engine": "test"},
                {"url": "https://example.com/2", "title": "Job 2", "content": "B", "engine": "test"},
            ]
        }
        return httpx.Response(200, json=payload)

    transport = httpx.MockTransport(handler)
    client = SearXngClient("http://searxng", transport=transport)
    results = await client.search("query", max_results=2)
    await client.close()

    assert len(results) == 2
    assert results[0].url == "https://example.com/1"
    assert results[1].title == "Job 2"
