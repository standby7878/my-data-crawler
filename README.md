# my-data-crawler

End-to-end pipeline for discovering job posting URLs via SearXNG, crawling HTML pages, and running a simple extractor + exports via a Java Spring Boot orchestrator.

## Sub-projects

- `crawler/` (Python): URL discovery + HTML fetching. See `crawler/README.md`.
- `orchestrator/` (Java): REST API, run orchestration, H2 state, and exports. See `orchestrator/README.md`.
- `scripts/`: helper scripts to start SearXNG and the orchestrator.

## Quickstart (local)

Prereqs: Docker (for SearXNG), Java 21, Python 3.12.

1) Start SearXNG (defaults to `http://localhost:8080`)

```bash
./scripts/start-searxng.sh
export SEARXNG_BASE_URL=http://localhost:8080
```

`./scripts/start-searxng.sh` always recreates the container to ensure config changes in `scripts/searxng/settings.yml` are applied.
The bundled config disables some non-job engines (e.g. `wikipedia`) and problematic defaults (e.g. `ahmia`, `torch`).

2) Build + start orchestrator (defaults to `http://localhost:8081`)

```bash
cd orchestrator
./gradlew bootJar
cd ..
./scripts/start-orchestrator.sh
```

Open Swagger UI: `http://localhost:8081/swagger-ui.html`

3) Crawl pages into `jobs/YYYY-MM-DD`

```bash
cd crawler
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

PYTHONPATH=src python -m crawler run \
  --date 2026-01-21 \
  --tier both \
  --searxng-url "$SEARXNG_BASE_URL" \
  --seed-companies src/crawler/discovery/seed_companies.yaml \
  --out ../jobs/2026-01-21
cd ..
```

4) Run extraction + export via orchestrator

```bash
curl -sS -X POST http://localhost:8081/api/runs/start \
  -H 'Content-Type: application/json' \
  -d '{"runDate":"2026-01-21","inputDir":"jobs/2026-01-21","runsDir":"runs","exportsDir":"exports"}'
```

Optional: orchestrator-driven discovery + crawl (writes under `/tmp`)

```bash
curl -sS -X POST http://localhost:8081/api/v1/e2e/search-jobs-crawl \
  -H 'Content-Type: application/json' \
  -d '{"site":"all","query":"kesätyö opiskelija Uusimaa","max_results":5}'
```

Outputs:

- `jobs/YYYY-MM-DD/`: crawler artifacts (`*.html` + `*_meta.json`)
- `runs/YYYY-MM-DD/raw_extractions/`: orchestrator raw extraction JSON
- `exports/YYYY-MM-DD/`: `jobs.csv` and `jobs.jsonl`

These output directories (and `data/`) are gitignored by default.

## Tests

- Python: `cd crawler && pytest`
- Java: `cd orchestrator && ./gradlew test`
- E2E (requires running SearXNG + orchestrator):
  `cd crawler && SEARXNG_BASE_URL=... ORCHESTRATOR_BASE_URL=... pytest -k e2e_pipeline`
