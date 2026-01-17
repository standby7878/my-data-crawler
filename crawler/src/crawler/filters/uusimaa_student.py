from typing import Iterable

KEYWORDS = [
    "uusimaa",
    "helsinki",
    "espoo",
    "vantaa",
    "kesatyo",
    "harjoittelu",
    "opiskelija",
]


def filter_urls(urls: Iterable[str]) -> list[str]:
    return [url for url in urls if _matches(url)]


def _matches(url: str) -> bool:
    lowered = url.lower()
    return any(keyword in lowered for keyword in KEYWORDS)
