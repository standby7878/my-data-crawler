package com.mycrawler.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.dto.E2eSearchJobsCrawlRequest;
import com.mycrawler.orchestrator.dto.E2eSearchJobsCrawlResponse;
import com.mycrawler.orchestrator.dto.SearchJobsRequest;
import com.mycrawler.orchestrator.dto.SearchJobsResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class E2eSearchJobsCrawlService {
    private static final Logger logger = LoggerFactory.getLogger(E2eSearchJobsCrawlService.class);

    private final SearchJobsService searchJobsService;
    private final ObjectMapper objectMapper;

    public E2eSearchJobsCrawlService(SearchJobsService searchJobsService, ObjectMapper objectMapper) {
        this.searchJobsService = searchJobsService;
        this.objectMapper = objectMapper;
    }

    public E2eSearchJobsCrawlResponse run(E2eSearchJobsCrawlRequest request, SearxngService.ForwardedHeaders forwardedHeaders) {
        if (request == null) {
            return new E2eSearchJobsCrawlResponse(
                    LocalDate.now().toString(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Missing request body"
            );
        }
        String date = request != null && request.date() != null && !request.date().isBlank()
                ? request.date().trim()
                : LocalDate.now().toString();
        int maxResults = request != null && request.maxResults() != null ? request.maxResults() : 3;

        SearchJobsRequest searchJobsRequest = new SearchJobsRequest(request.site(), request.query(), maxResults);
        SearchJobsResponse searchResponse = searchJobsService.searchJobs(searchJobsRequest, forwardedHeaders);
        List<String> urls = searchJobsService.extractUrls(searchResponse);

        Path tmpDir = Path.of("/tmp", "my-data-crawler-e2e", Instant.now().toString().replace(":", "-") + "-" + UUID.randomUUID());
        Path urlsJsonlPath = tmpDir.resolve("urls.jsonl");
        Path crawlOutDir = tmpDir.resolve("crawl");
        Path stdoutLog = tmpDir.resolve("crawler.stdout.log");
        Path stderrLog = tmpDir.resolve("crawler.stderr.log");

        try {
            Files.createDirectories(tmpDir);
            Files.createDirectories(crawlOutDir);
            writeUrlsJsonl(urlsJsonlPath, urls);
        } catch (Exception ex) {
            return new E2eSearchJobsCrawlResponse(
                    date,
                    urls,
                    tmpDir.toString(),
                    urlsJsonlPath.toString(),
                    crawlOutDir.toString(),
                    stdoutLog.toString(),
                    stderrLog.toString(),
                    null,
                    null,
                    "Failed to prepare /tmp artifacts: " + ex.getMessage()
            );
        }

        long startMs = System.currentTimeMillis();
        Integer exitCode = null;
        String error = null;
        try {
            exitCode = runCrawler(date, urlsJsonlPath, crawlOutDir, stdoutLog, stderrLog);
        } catch (Exception ex) {
            error = "Crawler invocation failed: " + ex.getMessage();
            try {
                Files.writeString(stderrLog, error + "\n", StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        long durationMs = System.currentTimeMillis() - startMs;
        logger.info("E2E crawl done: date={} urls={} exitCode={} tmpDir={}", date, urls.size(), exitCode, tmpDir);

        return new E2eSearchJobsCrawlResponse(
                date,
                urls,
                tmpDir.toString(),
                urlsJsonlPath.toString(),
                crawlOutDir.toString(),
                stdoutLog.toString(),
                stderrLog.toString(),
                exitCode,
                durationMs,
                error
        );
    }

    private void writeUrlsJsonl(Path urlsJsonlPath, List<String> urls) throws IOException {
        Files.createDirectories(urlsJsonlPath.getParent());
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            Map<String, String> row = new HashMap<>();
            row.put("url", url);
            sb.append(objectMapper.writeValueAsString(row)).append('\n');
        }
        Files.writeString(urlsJsonlPath, sb.toString(), StandardCharsets.UTF_8);
    }

    private Integer runCrawler(String date, Path urlsJsonlPath, Path crawlOutDir, Path stdoutLog, Path stderrLog)
            throws IOException, InterruptedException {
        Path repoRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path crawlerDir = repoRoot.resolve("crawler");
        Path venvPython = crawlerDir.resolve(".venv").resolve("bin").resolve("python");
        String python = Files.exists(venvPython) ? venvPython.toString() : "python3";

        List<String> command = List.of(
                python,
                "-m",
                "crawler",
                "crawl",
                "--date",
                date,
                "--input",
                urlsJsonlPath.toString(),
                "--out",
                crawlOutDir.toString()
        );
        logger.info("Starting crawler: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(crawlerDir.toFile());
        pb.redirectOutput(stdoutLog.toFile());
        pb.redirectError(stderrLog.toFile());
        Map<String, String> env = pb.environment();
        env.put("PYTHONPATH", repoRoot.resolve("crawler").resolve("src").toString());

        Process process = pb.start();
        return process.waitFor();
    }
}
