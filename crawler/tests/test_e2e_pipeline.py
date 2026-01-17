import os
import time
import logging
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse
import httpx
import pytest
import asyncio

from crawler.discovery.searxng_client import SearXngClient
from crawler.discovery.tier1 import discover_aggregator_urls
from crawler.discovery.tier2 import discover_company_urls
from crawler.fetch.http_fetcher import fetch_urls
from crawler.fetch.robots import RobotsManager
from crawler.storage.writer import write_job_artifact


logger = logging.getLogger(__name__)


@pytest.mark.asyncio
async def test_e2e_pipeline_with_orchestrator(tmp_path: Path):
    searxng_base = os.getenv("SEARXNG_BASE_URL")
    orchestrator_base = os.getenv("ORCHESTRATOR_BASE_URL")
    if not searxng_base or not orchestrator_base:
        pytest.skip("SEARXNG_BASE_URL and ORCHESTRATOR_BASE_URL must be set")
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    logger.info("Using SEARXNG_BASE_URL=%s ORCHESTRATOR_BASE_URL=%s", searxng_base, orchestrator_base)

    client = SearXngClient(searxng_base)
    try:
        logger.info("Discovering tier1 URLs via SearXNG")
        tier1_urls = await discover_aggregator_urls(client, max_results=5)
        logger.info("Discovered %s tier1 URLs", len(tier1_urls))
        logger.info("Discovering tier2 URLs via SearXNG")
        tier2_urls = await discover_company_urls(
            client,
            seed_companies_path=Path("src/crawler/discovery/seed_companies.yaml"),
            max_results=5,
            expansion_path=None,
        )
        logger.info("Discovered %s tier2 URLs", len(tier2_urls))
    finally:
        await client.close()

    urls = list(dict.fromkeys(tier1_urls + tier2_urls))
    if not urls:
        pytest.skip("No URLs discovered via SearXNG")

    selected_urls = urls[:5]
    logger.info("Selected URLs: %s", selected_urls)
    user_agent = "Mozilla/5.0 (compatible; JobCrawler/0.1; +http://localhost)"
    robots_manager = RobotsManager(user_agent=user_agent)
    logger.info("Fetching %s URLs", len(selected_urls))
    results = await fetch_urls(
        selected_urls,
        concurrency=3,
        user_agent=user_agent,
        robots_manager=robots_manager,
        retries=2,
    )
    logger.info("Fetched %s pages", len(results))

    run_date = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    input_dir = tmp_path / "jobs" / run_date
    runs_dir = tmp_path / "runs"
    exports_dir = tmp_path / "exports"
    logger.info("Writing artifacts to %s", input_dir)

    crawl_time = datetime.now(timezone.utc).isoformat()
    for idx, result in enumerate(results, start=1):
        parsed = urlparse(result.url)
        source = parsed.netloc.replace(".", "-") or "job"
        item_id = f"{idx:05d}"
        meta = {"url": result.url, "crawl_time": crawl_time, "source": source}
        write_job_artifact(input_dir, source="e2e", item_id=item_id, html=result.content, meta=meta)

    async with httpx.AsyncClient(timeout=30) as http_client:
        logger.info("Starting orchestrator run")
        response = await http_client.post(
            f"{orchestrator_base}/api/runs/start",
            json={
                "runDate": run_date,
                "inputDir": str(input_dir),
                "runsDir": str(runs_dir),
                "exportsDir": str(exports_dir),
            },
        )
        response.raise_for_status()
        run_id = response.json()["id"]
        logger.info("Run id: %s", run_id)

        deadline = time.time() + 120
        status = None
        while time.time() < deadline:
            run_resp = await http_client.get(f"{orchestrator_base}/api/runs/{run_id}")
            if run_resp.status_code == 404:
                break
            run_data = run_resp.json()
            status = run_data.get("status")
            logger.debug("Run status: %s", status)
            if status in ("COMPLETED", "FAILED"):
                break
            await asyncio.sleep(2)

    assert status == "COMPLETED"
