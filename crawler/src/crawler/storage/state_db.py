from __future__ import annotations

import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional


@dataclass
class UrlState:
    normalized_url: str
    final_url: Optional[str]
    canonical_url: Optional[str]
    etag: Optional[str]
    last_modified: Optional[str]
    last_fetch: Optional[str]
    status: Optional[str]
    http_status: Optional[int]
    content_type: Optional[str]
    rejected_reason: Optional[str]
    blocked_reason: Optional[str]
    classification_type: Optional[str]
    classification_confidence: Optional[float]


class StateDb:
    def __init__(self, path: Path) -> None:
        self.path = path
        self._init_db()

    def _init_db(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS url_state (
                    normalized_url TEXT PRIMARY KEY,
                    final_url TEXT,
                    canonical_url TEXT,
                    etag TEXT,
                    last_modified TEXT,
                    last_fetch TEXT,
                    status TEXT,
                    http_status INTEGER,
                    content_type TEXT,
                    rejected_reason TEXT,
                    blocked_reason TEXT,
                    classification_type TEXT,
                    classification_confidence REAL
                )
                """
            )

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.path)

    def get(self, normalized_url: str) -> Optional[UrlState]:
        with self._connect() as conn:
            row = conn.execute(
                "SELECT normalized_url, final_url, canonical_url, etag, last_modified, last_fetch, status, "
                "http_status, content_type, rejected_reason, blocked_reason, classification_type, "
                "classification_confidence FROM url_state WHERE normalized_url = ?",
                (normalized_url,),
            ).fetchone()
        if not row:
            return None
        return UrlState(*row)

    def upsert_fetch(
        self,
        normalized_url: str,
        *,
        final_url: Optional[str],
        canonical_url: Optional[str],
        etag: Optional[str],
        last_modified: Optional[str],
        status: Optional[str],
        http_status: Optional[int],
        content_type: Optional[str],
        rejected_reason: Optional[str] = None,
        blocked_reason: Optional[str] = None,
    ) -> None:
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO url_state (
                    normalized_url, final_url, canonical_url, etag, last_modified, last_fetch, status,
                    http_status, content_type, rejected_reason, blocked_reason, classification_type,
                    classification_confidence
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(normalized_url) DO UPDATE SET
                    final_url=excluded.final_url,
                    canonical_url=excluded.canonical_url,
                    etag=excluded.etag,
                    last_modified=excluded.last_modified,
                    last_fetch=excluded.last_fetch,
                    status=excluded.status,
                    http_status=excluded.http_status,
                    content_type=excluded.content_type,
                    rejected_reason=excluded.rejected_reason,
                    blocked_reason=excluded.blocked_reason
                """,
                (
                    normalized_url,
                    final_url,
                    canonical_url,
                    etag,
                    last_modified,
                    now,
                    status,
                    http_status,
                    content_type,
                    rejected_reason,
                    blocked_reason,
                    None,
                    None,
                ),
            )

    def update_classification(
        self,
        normalized_url: str,
        *,
        classification_type: Optional[str],
        confidence: Optional[float],
    ) -> None:
        with self._connect() as conn:
            conn.execute(
                "UPDATE url_state SET classification_type = ?, classification_confidence = ? WHERE normalized_url = ?",
                (classification_type, confidence, normalized_url),
            )

    def should_fetch(self, normalized_url: str, ttl_days: int) -> bool:
        state = self.get(normalized_url)
        if not state or not state.last_fetch:
            return True
        try:
            last_fetch = datetime.fromisoformat(state.last_fetch)
        except ValueError:
            return True
        age = datetime.now(timezone.utc) - last_fetch
        return age.days >= ttl_days

    def conditional_headers(self, normalized_url: str) -> dict[str, str]:
        state = self.get(normalized_url)
        headers: dict[str, str] = {}
        if not state:
            return headers
        if state.etag:
            headers["If-None-Match"] = state.etag
        if state.last_modified:
            headers["If-Modified-Since"] = state.last_modified
        return headers
