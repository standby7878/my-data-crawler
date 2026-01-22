from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from typing import Any


@dataclass
class ClassificationResult:
    payload: dict
    raw_output: str
    error: str | None = None


def classify_capsule(
    capsule: dict,
    *,
    model: str,
    use_search: bool,
    timeout: int = 30,
) -> ClassificationResult:
    prompt = _build_prompt(capsule)
    cmd = ["codex", "--non-interactive", "--model", model]
    if use_search:
        cmd.append("--search")
    try:
        result = subprocess.run(
            cmd,
            input=prompt,
            text=True,
            capture_output=True,
            check=False,
            timeout=timeout,
        )
    except FileNotFoundError:
        return ClassificationResult(payload={}, raw_output="", error="codex-cli-not-found")
    except subprocess.TimeoutExpired:
        return ClassificationResult(payload={}, raw_output="", error="codex-cli-timeout")

    output = (result.stdout or "").strip()
    parsed = _parse_json_from_output(output)
    if parsed is None:
        return ClassificationResult(payload={}, raw_output=output, error="codex-cli-invalid-json")
    return ClassificationResult(payload=parsed, raw_output=output)


def _build_prompt(capsule: dict) -> str:
    capsule_json = json.dumps(capsule, ensure_ascii=False)
    return (
        "You are a classifier for job pages. "
        "Return ONLY valid JSON with keys: type, confidence, fields.\n"
        "type must be one of: job_posting, listing, careers_hub, application_form, article, irrelevant, blocked.\n"
        "confidence is a number between 0 and 1.\n"
        "fields is an object. If type is job_posting, include: title, organization, location, deadline, "
        "apply_method, required_documents, contact_details, language_requirements. Otherwise keep fields empty.\n"
        "No markdown, no prose.\n"
        f"CAPSULE={capsule_json}"
    )


def _parse_json_from_output(text: str) -> dict | None:
    if not text:
        return None
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        return None
    try:
        return json.loads(text[start : end + 1])
    except json.JSONDecodeError:
        return None
