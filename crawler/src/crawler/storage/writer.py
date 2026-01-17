import json
from pathlib import Path
from typing import Dict


def write_job_artifact(
    output_dir: Path,
    *,
    source: str,
    item_id: str,
    html: str,
    meta: Dict[str, str],
) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    html_path = output_dir / f"{source}_{item_id}.html"
    meta_path = output_dir / f"{source}_{item_id}_meta.json"
    html_path.write_text(html, encoding="utf-8")
    meta_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
