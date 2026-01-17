from __future__ import annotations

from typing import Iterable, List, Set
from .searxng_client import SearXngClient

AGGREGATOR_SITES = [
    "mol.fi",
    "duunitori.fi",
    "oikotie.fi",
    "te-palvelut.fi",
]

QUERY_PATTERNS = [
    "site:{site} kesatyo Uusimaa",
    "site:{site} opiskelija työ Helsinki",
    "site:{site} harjoittelu Espoo",
    "site:{site} kesatyo Vantaa",
]


async def discover_aggregator_urls(
    client: SearXngClient,
    *,
    lang: str = "fi",
    max_results: int = 50,
) -> List[str]:
    urls: Set[str] = set()
    for site in AGGREGATOR_SITES:
        for pattern in QUERY_PATTERNS:
            query = pattern.format(site=site)
            results = await client.search(query, lang=lang, max_results=max_results)
            for result in results:
                if result.url:
                    urls.add(result.url)
    return sorted(urls)
