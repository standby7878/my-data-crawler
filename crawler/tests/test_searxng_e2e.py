import os
import pytest
import httpx

from crawler.discovery.searxng_client import SearXngClient


@pytest.mark.asyncio
async def test_searxng_e2e_search():
    base_url = os.getenv("SEARXNG_BASE_URL")
    if not base_url:
        pytest.skip("SEARXNG_BASE_URL not set")
    client = SearXngClient(base_url)
    try:
        results = await client.search("test", max_results=5)
    except httpx.HTTPStatusError as exc:
        if exc.response is not None and exc.response.status_code == 403:
            pytest.skip("SearXNG returned 403; check instance settings")
        raise
    finally:
        await client.close()
    assert results is not None
    assert isinstance(results, list)
