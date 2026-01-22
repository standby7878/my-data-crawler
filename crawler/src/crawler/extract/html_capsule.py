from __future__ import annotations

import json
import re
from typing import Iterable
from urllib.parse import urlparse

from bs4 import BeautifulSoup

from crawler.utils.url_utils import resolve_url, strip_fragment


APPLY_KEYWORDS = [
    "apply",
    "hakemus",
    "hae",
    "haku",
    "rekry",
    "careers",
    "open positions",
    "työpaikat",
    "uramahdollisuudet",
]

JOB_LINK_KEYWORDS = [
    "job",
    "jobs",
    "career",
    "positions",
    "vacancy",
    "rekry",
    "tyopaikat",
    "työpaikat",
]

DEADLINE_PATTERNS = [
    re.compile(r"\b(\d{4}-\d{2}-\d{2})\b"),
    re.compile(r"\b(\d{1,2}\.\d{1,2}\.\d{4})\b"),
    re.compile(r"\b(\d{1,2}/\d{1,2}/\d{4})\b"),
]


def build_capsule(html: str, base_url: str | None = None, max_text: int = 1200) -> dict:
    soup = BeautifulSoup(html or "", "lxml")
    _strip_noise(soup)
    title = soup.title.get_text(strip=True) if soup.title else None
    meta_desc = None
    meta_tag = soup.find("meta", attrs={"name": "description"})
    if meta_tag and meta_tag.get("content"):
        meta_desc = meta_tag.get("content").strip()
    h1_tag = soup.find("h1")
    h1_text = h1_tag.get_text(strip=True) if h1_tag else None
    text_snippet = _extract_visible_text(soup, max_text=max_text)
    apply_links = _extract_apply_links(soup, base_url)
    structured_job = _has_jobposting_jsonld(soup)
    deadline_candidates = _extract_deadlines(text_snippet or "")
    canonical_url = _extract_canonical(soup, base_url)
    return {
        "title": title,
        "meta_description": meta_desc,
        "h1": h1_text,
        "text_snippet": text_snippet,
        "apply_links": apply_links,
        "deadline_candidates": deadline_candidates,
        "structured_jobposting": structured_job,
        "canonical_url": canonical_url,
    }


def extract_job_links(html: str, base_url: str | None = None, limit: int = 50) -> list[str]:
    soup = BeautifulSoup(html or "", "lxml")
    _strip_noise(soup)
    links: list[str] = []
    for anchor in soup.find_all("a"):
        href = anchor.get("href")
        if not href:
            continue
        text = anchor.get_text(" ", strip=True).lower()
        candidate = resolve_url(base_url or "", href)
        candidate = strip_fragment(candidate)
        if any(keyword in text for keyword in JOB_LINK_KEYWORDS) or _keyword_in_path(candidate):
            links.append(candidate)
        if len(links) >= limit:
            break
    return links


def is_thin_content(html: str, min_text_length: int = 200) -> bool:
    soup = BeautifulSoup(html or "", "lxml")
    _strip_noise(soup)
    text = _extract_visible_text(soup, max_text=min_text_length + 1)
    return len(text or "") < min_text_length


def _strip_noise(soup: BeautifulSoup) -> None:
    for tag in soup(["script", "style", "noscript", "svg"]):
        tag.extract()


def _extract_visible_text(soup: BeautifulSoup, max_text: int) -> str | None:
    chunks: list[str] = []
    total = 0
    for chunk in soup.stripped_strings:
        if not chunk:
            continue
        chunks.append(chunk)
        total += len(chunk) + 1
        if total >= max_text:
            break
    text = " ".join(chunks)
    return text[:max_text] if text else None


def _extract_apply_links(soup: BeautifulSoup, base_url: str | None) -> list[dict]:
    links: list[dict] = []
    for anchor in soup.find_all("a"):
        href = anchor.get("href")
        if not href:
            continue
        text = anchor.get_text(" ", strip=True)
        lowered = text.lower()
        if any(keyword in lowered for keyword in APPLY_KEYWORDS):
            links.append(
                {
                    "text": text,
                    "url": strip_fragment(resolve_url(base_url or "", href)),
                }
            )
        if len(links) >= 5:
            break
    return links


def _has_jobposting_jsonld(soup: BeautifulSoup) -> bool:
    for script in soup.find_all("script", attrs={"type": "application/ld+json"}):
        try:
            payload = json.loads(script.get_text(strip=True))
        except json.JSONDecodeError:
            continue
        if _contains_jobposting(payload):
            return True
    return False


def _contains_jobposting(payload: object) -> bool:
    if isinstance(payload, dict):
        if payload.get("@type") == "JobPosting":
            return True
        return any(_contains_jobposting(value) for value in payload.values())
    if isinstance(payload, list):
        return any(_contains_jobposting(item) for item in payload)
    return False


def _extract_deadlines(text: str) -> list[str]:
    candidates: list[str] = []
    for pattern in DEADLINE_PATTERNS:
        for match in pattern.findall(text):
            candidates.append(match)
    return candidates[:5]


def _extract_canonical(soup: BeautifulSoup, base_url: str | None) -> str | None:
    link = soup.find("link", rel="canonical")
    if link and link.get("href"):
        return strip_fragment(resolve_url(base_url or "", link["href"]))
    return None


def _keyword_in_path(url: str) -> bool:
    try:
        path = urlparse(url).path.lower()
    except ValueError:
        return False
    return any(keyword in path for keyword in JOB_LINK_KEYWORDS)
