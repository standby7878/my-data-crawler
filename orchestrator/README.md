# Orchestrator (Spring Boot)

Java Spring Boot backend that:

- exposes a small REST API (Swagger/OpenAPI),
- talks to a SearXNG instance for search,
- runs extraction/export jobs over crawler output,
- stores run state in an embedded H2 database.

## Requirements

- Java 21+
- Docker (optional, only if you run SearXNG locally)

## Build and run

Build a runnable JAR:

```bash
cd orchestrator
./gradlew bootJar
```

Run the API (defaults to port `8081`):

```bash
cd ..
./scripts/start-orchestrator.sh
```

Open Swagger UI: `http://localhost:8081/swagger-ui.html`

## Configuration

Defaults live in `orchestrator/src/main/resources/application.yml`.

- `SEARXNG_BASE_URL`: SearXNG base URL (default `http://localhost:8080`)
- `SEARXNG_USER_AGENT`: user agent forwarded to SearXNG
- `SEARXNG_ENGINES_PRIMARY`: preferred engines (default `google,bing,brave`)
- `SEARXNG_ENGINES_FALLBACK`: fallback engines (default `qwant,mojeek`)
- `server.port`: defaults to `8081`

If you run SearXNG via `./scripts/start-searxng.sh`, the instance is configured via `scripts/searxng/settings.yml`.

Database:

- Default datasource is `jdbc:h2:file:./data/orchestrator` (relative to the process working directory).
- Override with `SPRING_DATASOURCE_URL` if you want the DB somewhere else.

## API overview

- `GET /api/health`: simple health check (`{"status":"ok"}`)
- `GET /api/searxng/health`: connectivity check against SearXNG
- `POST /api/v1/search`: generic SearXNG search proxy
- `POST /api/v1/search-jobs`: job-focused search across one or more sites
- `POST /api/v1/e2e/search-jobs-crawl`: searches via SearXNG and invokes the Python crawler (writes artifacts under `/tmp`)
- `POST /api/runs/start`: run extraction + export for crawler HTML in `inputDir`
- `POST /api/runs/reprocess`: regenerate exports from existing `runsDir/.../raw_extractions`
- `POST /api/runs/apply-review`: apply edits from a review CSV, then rewrite exports
- `GET /api/runs`: list runs
- `GET /api/runs/{id}`: get run status/details

## Job search behavior

`POST /api/v1/search-jobs` is optimized for job discovery:

- Uses strict `site:domain` queries (domain restriction).
- Runs multiple query variants per site (Finnish is the default).
- Tries `SEARXNG_ENGINES_PRIMARY` first and only uses `SEARXNG_ENGINES_FALLBACK` if results are still under the requested `max_results`.
- Selects SearXNG `lang` based on the prompt (defaults to `fi`).

The `POST /api/v1/e2e/search-jobs-crawl` endpoint requires the crawler dependencies to be installed (it prefers `crawler/.venv/bin/python` when present).

## Runs: inputs and outputs

`POST /api/runs/start` expects:

- `runDate` (YYYY-MM-DD)
- `inputDir`: directory containing crawler artifacts:
  - `*.html` (downloaded page)
  - matching `*_meta.json` (optional metadata; if missing, extraction still runs)
- `runsDir`: where raw extractions are written:
  - `runs/<runDate>/raw_extractions/*.json`
- `exportsDir`: where final exports are written:
  - `exports/<runDate>/jobs.jsonl`
  - `exports/<runDate>/jobs.csv`

## Review CSV format

`POST /api/runs/apply-review` reads `exports/<runDate>/jobs.jsonl`, applies edits from the provided CSV, and rewrites exports.

Expected header columns (extra columns are ignored):

- `job_id`
- `job_title`
- `company_name`
- `location_municipality`
- `source_url`
- `confidence`

Only non-empty values overwrite existing fields.
