import pytest

from crawler.discovery.tier1 import discover_aggregator_urls
from crawler.discovery.searxng_client import SearchResult


class FakeClient:
    async def search(self, query, lang="fi", max_results=50):
        return [
            SearchResult(url="https://duunitori.fi/job/1", title="Job", content="x"),
            SearchResult(url="https://mol.fi/job/2", title="Job", content="x"),
        ]


@pytest.mark.asyncio
async def test_discover_aggregator_urls_returns_unique_urls():
    urls = await discover_aggregator_urls(FakeClient())
    assert "https://duunitori.fi/job/1" in urls
    assert "https://mol.fi/job/2" in urls
    assert len(urls) == 2
