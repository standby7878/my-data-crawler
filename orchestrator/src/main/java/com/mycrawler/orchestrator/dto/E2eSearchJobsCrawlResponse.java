package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "E2E response: selected URLs, temp paths and crawler run metadata")
public record E2eSearchJobsCrawlResponse(
        @Schema(description = "Resolved run date (YYYY-MM-DD)")
        String date,
        @Schema(description = "Selected URLs from SearXNG (deduplicated)")
        List<String> urls,
        @Schema(description = "Base temp directory under /tmp")
        @JsonProperty("tmp_dir")
        String tmpDir,
        @Schema(description = "Written urls.jsonl path")
        @JsonProperty("urls_jsonl_path")
        String urlsJsonlPath,
        @Schema(description = "Crawler output directory")
        @JsonProperty("crawl_out_dir")
        String crawlOutDir,
        @Schema(description = "Crawler stdout log path")
        @JsonProperty("crawler_stdout_log")
        String crawlerStdoutLog,
        @Schema(description = "Crawler stderr log path")
        @JsonProperty("crawler_stderr_log")
        String crawlerStderrLog,
        @Schema(description = "Crawler exit code (null if not started)")
        @JsonProperty("crawler_exit_code")
        Integer crawlerExitCode,
        @Schema(description = "Crawler duration in milliseconds")
        @JsonProperty("crawler_duration_ms")
        Long crawlerDurationMs,
        @Schema(description = "Error message if crawler failed to start or run")
        String error
) {
}

