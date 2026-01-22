from __future__ import annotations

from typing import Iterable, Iterator
from urllib.parse import parse_qsl, urljoin, urlsplit, urlunsplit


TRACKING_PARAMS = {
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_term",
    "utm_content",
    "gclid",
    "fbclid",
    "yclid",
    "mc_cid",
    "mc_eid",
    "igshid",
    "msclkid",
}


def is_http_url(url: str) -> bool:
    try:
        parts = urlsplit(url)
    except ValueError:
        return False
    return parts.scheme in {"http", "https"} and bool(parts.netloc)


def normalize_url(url: str) -> str:
    parts = urlsplit(url.strip())
    scheme = parts.scheme.lower()
    netloc = parts.netloc.lower()
    path = parts.path or "/"
    query = _normalize_query(parts.query)
    return urlunsplit((scheme, netloc, path, query, ""))


def _normalize_query(query: str) -> str:
    pairs = []
    for key, value in parse_qsl(query, keep_blank_values=True):
        lowered = key.lower()
        if lowered in TRACKING_PARAMS:
            continue
        if lowered.startswith("utm_"):
            continue
        pairs.append((lowered, value))
    pairs.sort()
    return "&".join(f"{key}={value}" if value else key for key, value in pairs)


def strip_fragment(url: str) -> str:
    parts = urlsplit(url)
    return urlunsplit((parts.scheme, parts.netloc, parts.path, parts.query, ""))


def resolve_url(base_url: str, link: str) -> str:
    return urljoin(base_url, link)


def same_domain(url_a: str, url_b: str) -> bool:
    return urlsplit(url_a).netloc.lower() == urlsplit(url_b).netloc.lower()


def iter_deduped(urls: Iterable[str]) -> Iterator[str]:
    seen: set[str] = set()
    for url in urls:
        if not url:
            continue
        normalized = normalize_url(strip_fragment(url))
        if normalized in seen:
            continue
        seen.add(normalized)
        yield url
