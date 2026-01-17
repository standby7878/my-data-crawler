# Repository Guidelines

## Project Structure & Module Organization
- `orchestrator/`: Java Spring Boot backend (REST API + H2). Source in `orchestrator/src/main/java`, config in `orchestrator/src/main/resources`, tests in `orchestrator/src/test/java`.
- `crawler/`: Python crawler and discovery. Code in `crawler/src/crawler`, tests in `crawler/tests`, venv in `crawler/.venv`.
- `scripts/`: helper scripts (e.g., `scripts/start-searxng.sh`, `scripts/start-orchestrator.sh`).
- Outputs during runs: `jobs/YYYY-MM-DD`, `runs/YYYY-MM-DD`, `exports/YYYY-MM-DD` (created by the pipeline).

## Build, Test, and Development Commands
- Build Java JAR: `cd orchestrator && ./gradlew bootJar`.
- Run Java API: `scripts/start-orchestrator.sh` (uses G1GC and heap defaults).
- Start SearXNG container: `scripts/start-searxng.sh`.
- Python venv: `cd crawler && source .venv/bin/activate`.
- Install Python deps: `pip install -r requirements.txt`.
- Python tests: `pytest` (run inside `crawler/`).
- Java tests: `cd orchestrator && ./gradlew test`.

## Coding Style & Naming Conventions
- Java: 4-space indentation, `CamelCase` classes, `lowerCamelCase` methods/fields. Keep REST DTOs in `dto/`.
- Python: 4-space indentation, `snake_case` modules/functions, `CamelCase` classes.
- Logging: use `org.slf4j.Logger` in Java; Python uses `logging` (default DEBUG for crawler).

## Testing Guidelines
- Java: JUnit 5 under `orchestrator/src/test/java`.
- Python: pytest under `crawler/tests`, test files named `test_*.py`.
- Run E2E: `SEARXNG_BASE_URL=... ORCHESTRATOR_BASE_URL=... pytest -k e2e_pipeline`.

## Commit & Pull Request Guidelines
- No strict convention defined; use short, imperative summaries (e.g., "add searxng search endpoint").
- Keep commits focused and include a brief PR description with test results or reproduction steps.

## Security & Configuration Tips
- SearXNG base URL is configured via `SEARXNG_BASE_URL` (default `http://localhost:8080`).
- Orchestrator runs on port 8081 by default (`orchestrator/src/main/resources/application.yml`).
- Avoid committing large crawl outputs; keep generated files in run directories.
