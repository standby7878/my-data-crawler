package com.mycrawler.orchestrator.service;

import com.mycrawler.orchestrator.db.RunEntity;
import com.mycrawler.orchestrator.db.RunEvent;
import com.mycrawler.orchestrator.db.RunEventRepository;
import com.mycrawler.orchestrator.db.RunRepository;
import com.mycrawler.orchestrator.db.RunStatus;
import com.mycrawler.orchestrator.db.RunType;
import com.mycrawler.orchestrator.dto.ApplyReviewRequest;
import com.mycrawler.orchestrator.dto.ReprocessRequest;
import com.mycrawler.orchestrator.dto.RunResponse;
import com.mycrawler.orchestrator.dto.RunSummary;
import com.mycrawler.orchestrator.dto.StartRunRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunService {
    private static final Logger logger = LoggerFactory.getLogger(RunService.class);
    private final RunRepository runRepository;
    private final RunEventRepository runEventRepository;
    private final RunProcessor runProcessor;

    public RunService(RunRepository runRepository, RunEventRepository runEventRepository, RunProcessor runProcessor) {
        this.runRepository = runRepository;
        this.runEventRepository = runEventRepository;
        this.runProcessor = runProcessor;
    }

    public RunResponse startRun(StartRunRequest request) {
        RunEntity run = new RunEntity();
        run.setRunType(RunType.FULL);
        run.setStatus(RunStatus.QUEUED);
        run.setRunDate(request.runDate());
        run.setInputDir(request.inputDir());
        run.setRunsDir(request.runsDir());
        run.setExportsDir(request.exportsDir());
        run.setCreatedAt(Instant.now());
        run = runRepository.save(run);
        recordEvent(run, "RUN_CREATED", "Run queued by user");
        logger.info("Queued run {} type={} date={}", run.getId(), run.getRunType(), run.getRunDate());
        runProcessor.processRun(run.getId());
        return toResponse(run);
    }

    public RunResponse reprocess(ReprocessRequest request) {
        RunEntity run = new RunEntity();
        run.setRunType(RunType.REPROCESS);
        run.setStatus(RunStatus.QUEUED);
        run.setRunDate(request.runDate());
        run.setInputDir("n/a");
        run.setRunsDir(request.runsDir());
        run.setExportsDir(request.exportsDir());
        run.setCreatedAt(Instant.now());
        run = runRepository.save(run);
        recordEvent(run, "REPROCESS_CREATED", "Reprocess queued by user");
        logger.info("Queued run {} type={} date={}", run.getId(), run.getRunType(), run.getRunDate());
        runProcessor.processRun(run.getId());
        return toResponse(run);
    }

    public RunResponse applyReview(ApplyReviewRequest request) {
        RunEntity run = new RunEntity();
        run.setRunType(RunType.APPLY_REVIEW);
        run.setStatus(RunStatus.QUEUED);
        run.setRunDate(request.runDate());
        run.setInputDir("n/a");
        run.setRunsDir("n/a");
        run.setExportsDir(request.exportsDir());
        run.setReviewCsvPath(request.reviewCsvPath());
        run.setCreatedAt(Instant.now());
        run = runRepository.save(run);
        recordEvent(run, "APPLY_REVIEW_CREATED", "Review apply queued by user");
        logger.info("Queued run {} type={} date={}", run.getId(), run.getRunType(), run.getRunDate());
        runProcessor.processRun(run.getId());
        return toResponse(run);
    }

    public Optional<RunResponse> getRun(Long id) {
        return runRepository.findById(id).map(this::toResponse);
    }

    public List<RunSummary> listRuns() {
        return runRepository.findAll().stream()
                .map(run -> new RunSummary(
                        run.getId(),
                        run.getRunType(),
                        run.getStatus(),
                        run.getRunDate(),
                        run.getCreatedAt(),
                        run.getFinishedAt()))
                .toList();
    }

    private void recordEvent(RunEntity run, String eventType, String message) {
        RunEvent event = new RunEvent();
        event.setRun(run);
        event.setEventType(eventType);
        event.setMessage(message);
        event.setCreatedAt(Instant.now());
        runEventRepository.save(event);
    }

    private RunResponse toResponse(RunEntity run) {
        return new RunResponse(
                run.getId(),
                run.getRunType(),
                run.getStatus(),
                run.getRunDate(),
                run.getInputDir(),
                run.getRunsDir(),
                run.getExportsDir(),
                run.getReviewCsvPath(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getMessage());
    }
}
