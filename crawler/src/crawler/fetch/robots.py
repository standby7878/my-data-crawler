from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from typing import Dict, Optional
from urllib.parse import urljoin, urlparse
from urllib import robotparser
import httpx
import logging

logger = logging.getLogger(__name__)


@dataclass
class RobotsManager:
    user_agent: str
    timeout: int = 10
    _cache: Dict[str, robotparser.RobotFileParser] = field(default_factory=dict)
    _lock: asyncio.Lock = field(default_factory=asyncio.Lock)

    async def is_allowed(self, url: str) -> bool:
        parsed = urlparse(url)
        base = f"{parsed.scheme}://{parsed.netloc}"
        parser = await self._get_parser(base)
        return parser.can_fetch(self.user_agent, url)

    async def _get_parser(self, base_url: str) -> robotparser.RobotFileParser:
        async with self._lock:
            if base_url in self._cache:
                return self._cache[base_url]
            parser = robotparser.RobotFileParser()
            robots_url = urljoin(base_url, "/robots.txt")
            parser.set_url(robots_url)
            try:
                async with httpx.AsyncClient(timeout=httpx.Timeout(self.timeout)) as client:
                    response = await client.get(robots_url, headers={"User-Agent": self.user_agent})
                    if response.status_code == 200:
                        parser.parse(response.text.splitlines())
                    else:
                        parser.disallow_all = False
            except Exception as exc:
                logger.warning("Failed to fetch robots.txt from %s: %s", robots_url, exc)
                parser.disallow_all = False
            self._cache[base_url] = parser
            return parser
