from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, Iterable, List, Set
import yaml
from .searxng_client import SearXngClient

DEFAULT_PATTERNS = [
    "{name} tyopaikat",
    "{name} ura",
    "{name} rekrytointi",
    "{name} careers",
]

URL_PATH_PROBES = [
    "/tyopaikat",
    "/ura",
    "/rekrytointi",
    "/careers",
]


def load_seed_companies(path: Path) -> List[Dict[str, str]]:
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError("Seed companies file must be a list")
    return [item for item in data if isinstance(item, dict) and item.get("name")]


async def discover_company_urls(
    client: SearXngClient,
    seed_companies_path: Path,
    *,
    lang: str = "fi",
    max_results: int = 30,
    expansion_path: Path | None = None,
) -> List[str]:
    companies = load_seed_companies(seed_companies_path)
    urls: Set[str] = set()
    for company in companies:
        name = company["name"]
        queries = [pattern.format(name=name) for pattern in DEFAULT_PATTERNS]
        for query in queries:
            results = await client.search(query, lang=lang, max_results=max_results)
            for result in results:
                if not result.url:
                    continue
                urls.add(result.url)
                if expansion_path:
                    _append_expansion(expansion_path, name, result.url, "searxng")
        for domain in _candidate_domains(company):
            for path in URL_PATH_PROBES:
                url = f"{domain}{path}"
                urls.add(url)
                if expansion_path:
                    _append_expansion(expansion_path, name, url, "probe")
    return sorted(urls)


def _candidate_domains(company: Dict[str, str]) -> Iterable[str]:
    if company.get("domain"):
        domain = company["domain"].rstrip("/")
        if not domain.startswith("http"):
            domain = f"https://{domain}"
        return [domain]
    return []


def _append_expansion(path: Path, company: str, url: str, source: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps({"company": company, "url": url, "source": source}) + "\n")
