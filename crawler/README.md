# Crawler (Python)

Python discovery + crawling utilities that produce HTML artifacts the Java orchestrator can process.

## Requirements

- Python 3.12+
- A running SearXNG instance for discovery (default is usually `http://localhost:8080`)

## Setup

```bash
cd crawler
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

The package is executed via module mode with `PYTHONPATH=src`:

```bash
PYTHONPATH=src python -m crawler --help
```

## Commands

The CLI lives in `crawler/src/crawler/__main__.py` and exposes three subcommands:

### Discover URLs

Writes `urls.jsonl` to `--out` (JSON-lines: `{"url":"..."}`).

```bash
PYTHONPATH=src python -m crawler discover \
  --date 2026-01-21 \
  --tier both \
  --searxng-url http://localhost:8080 \
  --seed-companies src/crawler/discovery/seed_companies.yaml \
  --out ../jobs/2026-01-21
```

Discovery tiers:

- `tier1`: searches job aggregators (`mol.fi`, `duunitori.fi`, `oikotie.fi`, `te-palvelut.fi`) using predefined queries
- `tier2`: searches company names from `seed_companies.yaml` and probes common career paths on known domains

### Crawl URLs

Reads `--input` (a `urls.jsonl`) and writes `*.html` + `*_meta.json` artifacts to `--out`.

```bash
PYTHONPATH=src python -m crawler crawl \
  --date 2026-01-21 \
  --input ../jobs/2026-01-21/urls.jsonl \
  --out ../jobs/2026-01-21
```

### Discover + crawl (one command)

```bash
PYTHONPATH=src python -m crawler run \
  --date 2026-01-21 \
  --tier both \
  --searxng-url http://localhost:8080 \
  --seed-companies src/crawler/discovery/seed_companies.yaml \
  --out ../jobs/2026-01-21
```

## Output format

For each fetched URL the crawler writes:

- `<source>_<item_id>.html`
- `<source>_<item_id>_meta.json`

The orchestrator expects the `*_meta.json` naming convention next to the HTML.

## URL filtering

Discovered URLs are filtered through `crawler/src/crawler/filters/uusimaa_student.py` (keyword-based).

## Tests

```bash
cd crawler
pytest
```

E2E tests (require running SearXNG + orchestrator):

```bash
cd crawler
SEARXNG_BASE_URL=http://localhost:8080 ORCHESTRATOR_BASE_URL=http://localhost:8081 pytest -k e2e_pipeline
```

Note: URL discovery can also be done via the orchestrator’s `POST /api/v1/search-jobs` endpoint, which implements strict `site:` queries, engine tiering, and multiple prompt variants.
