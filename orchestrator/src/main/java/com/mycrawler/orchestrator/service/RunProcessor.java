package com.mycrawler.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.db.ExtractionRecord;
import com.mycrawler.orchestrator.db.ExtractionRecordRepository;
import com.mycrawler.orchestrator.db.ExtractionStatus;
import com.mycrawler.orchestrator.db.RunEntity;
import com.mycrawler.orchestrator.db.RunEvent;
import com.mycrawler.orchestrator.db.RunEventRepository;
import com.mycrawler.orchestrator.db.RunRepository;
import com.mycrawler.orchestrator.db.RunStatus;
import com.mycrawler.orchestrator.db.RunType;
import com.mycrawler.orchestrator.run.BatchScanner;
import com.mycrawler.orchestrator.run.JobPosting;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RunProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RunProcessor.class);
    private final RunRepository runRepository;
    private final RunEventRepository runEventRepository;
    private final ExtractionRecordRepository extractionRecordRepository;
    private final ExtractionService extractionService;
    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    public RunProcessor(
            RunRepository runRepository,
            RunEventRepository runEventRepository,
            ExtractionRecordRepository extractionRecordRepository,
            ExtractionService extractionService,
            ExportService exportService,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.runEventRepository = runEventRepository;
        this.extractionRecordRepository = extractionRecordRepository;
        this.extractionService = extractionService;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    @Async("runExecutor")
    public void processRun(Long runId) {
        RunEntity run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            return;
        }
        logger.info("Processing run {} type={} date={}", run.getId(), run.getRunType(), run.getRunDate());
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);
        recordEvent(run, "RUN_STARTED", "Run started");

        try {
            if (run.getRunType() == RunType.FULL) {
                processFullRun(run);
            } else if (run.getRunType() == RunType.REPROCESS) {
                processReprocess(run);
            } else if (run.getRunType() == RunType.APPLY_REVIEW) {
                processApplyReview(run);
            }
            run.setStatus(RunStatus.COMPLETED);
            run.setMessage("completed");
            logger.info("Run {} completed", run.getId());
        } catch (Exception ex) {
            run.setStatus(RunStatus.FAILED);
            run.setMessage(ex.getMessage());
            recordEvent(run, "RUN_FAILED", ex.getMessage());
            logger.error("Run {} failed: {}", run.getId(), ex.getMessage());
        } finally {
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
        }
    }

    private void processFullRun(RunEntity run) throws IOException {
        BatchScanner scanner = new BatchScanner();
        List<Path> htmlFiles = scanner.scanHtmlFiles(Path.of(run.getInputDir()));
        logger.info("Run {} found {} HTML files", run.getId(), htmlFiles.size());
        List<JobPosting> postings = new ArrayList<>();
        Path rawDir = Path.of(run.getRunsDir(), run.getRunDate(), "raw_extractions");
        for (Path htmlPath : htmlFiles) {
            JobPosting posting = extractionService.extractFromHtml(htmlPath);
            postings.add(posting);
            Path rawPath = rawDir.resolve(htmlPath.getFileName().toString().replace(".html", ".json"));
            extractionService.writeRawExtraction(rawPath, posting);
            saveRecord(run, htmlPath, posting, ExtractionStatus.SUCCESS);
        }
        exportService.writeExports(Path.of(run.getExportsDir()), run.getRunDate(), postings);
        recordEvent(run, "RUN_EXPORTED", "Exported " + postings.size() + " postings");
        logger.info("Run {} exported {} postings", run.getId(), postings.size());
    }

    private void processReprocess(RunEntity run) throws IOException {
        Path rawDir = Path.of(run.getRunsDir(), run.getRunDate(), "raw_extractions");
        List<JobPosting> postings = new ArrayList<>();
        if (Files.exists(rawDir)) {
            try (var stream = Files.list(rawDir)) {
                for (Path jsonPath : stream.filter(path -> path.toString().endsWith(".json")).toList()) {
                    JobPosting posting = objectMapper.readValue(jsonPath.toFile(), JobPosting.class);
                    postings.add(posting);
                }
            }
        }
        exportService.writeExports(Path.of(run.getExportsDir()), run.getRunDate(), postings);
        recordEvent(run, "RUN_REPROCESSED", "Reprocessed " + postings.size() + " postings");
        logger.info("Run {} reprocessed {} postings", run.getId(), postings.size());
    }

    private void processApplyReview(RunEntity run) throws IOException {
        Path reviewPath = Path.of(run.getReviewCsvPath());
        if (!Files.exists(reviewPath)) {
            recordEvent(run, "REVIEW_MISSING", "Review CSV not found");
            logger.warn("Run {} review CSV missing at {}", run.getId(), reviewPath);
            return;
        }
        Path exportDir = Path.of(run.getExportsDir(), run.getRunDate());
        Path jsonlPath = exportDir.resolve("jobs.jsonl");
        List<JobPosting> postings = new ArrayList<>();
        if (Files.exists(jsonlPath)) {
            try (BufferedReader reader = Files.newBufferedReader(jsonlPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    postings.add(objectMapper.readValue(line, JobPosting.class));
                }
            }
        }
        int updated = applyReviewEdits(reviewPath, postings);
        recordEvent(run, "REVIEW_APPLIED", "Review updates applied: " + updated);
        exportService.writeExports(Path.of(run.getExportsDir()), run.getRunDate(), postings);
        logger.info("Run {} applied review updates: {}", run.getId(), updated);
    }

    private void saveRecord(RunEntity run, Path htmlPath, JobPosting posting, ExtractionStatus status) {
        ExtractionRecord record = new ExtractionRecord();
        record.setRun(run);
        record.setSourcePath(htmlPath.toString());
        record.setSourceUrl(posting.getSourceUrl());
        record.setStatus(status);
        record.setConfidence(posting.getExtractionConfidence());
        record.setPayloadJson(writePayload(posting));
        record.setCreatedAt(Instant.now());
        extractionRecordRepository.save(record);
    }

    private String writePayload(JobPosting posting) {
        try {
            return objectMapper.writeValueAsString(posting);
        } catch (Exception ex) {
            return null;
        }
    }

    private void recordEvent(RunEntity run, String eventType, String message) {
        RunEvent event = new RunEvent();
        event.setRun(run);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setCreatedAt(Instant.now());
        runEventRepository.save(event);
    }

    private int applyReviewEdits(Path reviewPath, List<JobPosting> postings) throws IOException {
        Map<String, JobPosting> index = postings.stream()
                .filter(posting -> posting.getJobId() != null)
                .collect(java.util.stream.Collectors.toMap(JobPosting::getJobId, posting -> posting, (a, b) -> a));
        int updated = 0;
        try (var reader = Files.newBufferedReader(reviewPath);
             var parser = org.apache.commons.csv.CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            for (var record : parser) {
                String jobId = record.get("job_id");
                JobPosting posting = index.get(jobId);
                if (posting == null) {
                    continue;
                }
                updated += applyField(record, "job_title", posting.getJobTitle(), posting::setJobTitle) ? 1 : 0;
                updated += applyField(record, "company_name", posting.getCompanyName(), posting::setCompanyName) ? 1 : 0;
                updated += applyField(record, "location_municipality", posting.getLocationMunicipality(), posting::setLocationMunicipality) ? 1 : 0;
                updated += applyField(record, "source_url", posting.getSourceUrl(), posting::setSourceUrl) ? 1 : 0;
                updated += applyField(record, "confidence", posting.getExtractionConfidence(), posting::setExtractionConfidence) ? 1 : 0;
            }
        }
        return updated;
    }

    private boolean applyField(org.apache.commons.csv.CSVRecord record, String field, String current, java.util.function.Consumer<String> setter) {
        if (!record.isMapped(field)) {
            return false;
        }
        String value = record.get(field);
        if (value == null || value.isBlank() || value.equals(current)) {
            return false;
        }
        setter.accept(value);
        return true;
    }
}
