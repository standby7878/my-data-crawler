import argparse
import asyncio
import json
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

from crawler.discovery.searxng_client import SearXngClient
from crawler.discovery.tier1 import discover_aggregator_urls
from crawler.discovery.tier2 import discover_company_urls
from crawler.fetch.http_fetcher import fetch_urls
from crawler.fetch.robots import RobotsManager
from crawler.filters.uusimaa_student import filter_urls
from crawler.storage.writer import write_job_artifact
from crawler.utils.logging_utils import configure_logging


def _write_urls(path: Path, urls: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for url in urls:
            handle.write(json.dumps({"url": url}) + "\n")


def _load_urls(path: Path) -> list[str]:
    urls = []
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            data = json.loads(line)
            if data.get("url"):
                urls.append(data["url"])
    return urls


def _domain_from_url(url: str) -> str:
    return urlparse(url).netloc.replace(".", "-")


async def _discover(args: argparse.Namespace) -> list[str]:
    client = SearXngClient(args.searxng_url)
    urls: list[str] = []
    if args.tier in ("tier1", "both"):
        urls.extend(await discover_aggregator_urls(client, lang=args.lang))
    if args.tier in ("tier2", "both"):
        urls.extend(
            await discover_company_urls(
                client,
                seed_companies_path=Path(args.seed_companies),
                lang=args.lang,
                expansion_path=Path(args.seed_expansion) if args.seed_expansion else None,
            )
        )
    await client.close()
    urls = filter_urls(urls)
    return sorted(set(urls))


async def _crawl(args: argparse.Namespace, urls: list[str]) -> None:
    user_agent = "Mozilla/5.0 (compatible; JobCrawler/0.1; +http://localhost)"
    robots_manager = RobotsManager(user_agent=user_agent)
    results = await fetch_urls(
        urls,
        concurrency=args.concurrency,
        user_agent=user_agent,
        robots_manager=robots_manager,
        retries=args.retries,
        enforce_robots=False,
    )
    output_dir = Path(args.out)
    crawl_time = datetime.now(timezone.utc).isoformat()
    for idx, result in enumerate(results, start=1):
        source = _domain_from_url(result.url)
        item_id = f"{idx:05d}"
        meta = {
            "url": result.url,
            "crawl_time": crawl_time,
            "source": source,
        }
        write_job_artifact(output_dir, source=source, item_id=item_id, html=result.content, meta=meta)


def main() -> None:
    parser = argparse.ArgumentParser(description="Job crawler")
    parser.add_argument("--log-level", default="INFO")
    subparsers = parser.add_subparsers(dest="command", required=True)

    discover_parser = subparsers.add_parser("discover")
    discover_parser.add_argument("--date", required=True)
    discover_parser.add_argument("--tier", choices=["tier1", "tier2", "both"], default="both")
    discover_parser.add_argument("--searxng-url", required=True)
    discover_parser.add_argument("--out", required=True)
    discover_parser.add_argument("--lang", default="fi")
    discover_parser.add_argument("--seed-companies", default="crawler/src/crawler/discovery/seed_companies.yaml")
    discover_parser.add_argument("--seed-expansion")

    crawl_parser = subparsers.add_parser("crawl")
    crawl_parser.add_argument("--date", required=True)
    crawl_parser.add_argument("--input", required=True)
    crawl_parser.add_argument("--out", required=True)
    crawl_parser.add_argument("--concurrency", type=int, default=5)
    crawl_parser.add_argument("--retries", type=int, default=2)

    run_parser = subparsers.add_parser("run")
    run_parser.add_argument("--date", required=True)
    run_parser.add_argument("--tier", choices=["tier1", "tier2", "both"], default="both")
    run_parser.add_argument("--searxng-url", required=True)
    run_parser.add_argument("--out", required=True)
    run_parser.add_argument("--lang", default="fi")
    run_parser.add_argument("--seed-companies", default="crawler/src/crawler/discovery/seed_companies.yaml")
    run_parser.add_argument("--seed-expansion")
    run_parser.add_argument("--concurrency", type=int, default=5)
    run_parser.add_argument("--retries", type=int, default=2)

    args = parser.parse_args()
    configure_logging(args.log_level)

    if args.command == "discover":
        urls = asyncio.run(_discover(args))
        output_path = Path(args.out) / "urls.jsonl"
        _write_urls(output_path, urls)
    elif args.command == "crawl":
        urls = _load_urls(Path(args.input))
        asyncio.run(_crawl(args, urls))
    elif args.command == "run":
        urls = asyncio.run(_discover(args))
        output_path = Path(args.out) / "urls.jsonl"
        _write_urls(output_path, urls)
        asyncio.run(_crawl(args, urls))


if __name__ == "__main__":
    main()
