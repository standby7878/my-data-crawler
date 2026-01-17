package com.mycrawler.orchestrator.controller;

import com.mycrawler.orchestrator.dto.ApplyReviewRequest;
import com.mycrawler.orchestrator.dto.ReprocessRequest;
import com.mycrawler.orchestrator.dto.RunResponse;
import com.mycrawler.orchestrator.dto.RunSummary;
import com.mycrawler.orchestrator.dto.StartRunRequest;
import com.mycrawler.orchestrator.service.RunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
@Validated
@Tag(name = "Runs", description = "Run orchestration endpoints")
public class RunController {
    private static final Logger logger = LoggerFactory.getLogger(RunController.class);
    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @Operation(summary = "Start a full extraction run")
    @ApiResponse(responseCode = "200", description = "Run created",
            content = @Content(schema = @Schema(implementation = RunResponse.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "StartRunRequest",
                            value = "{\"runDate\":\"2026-01-17\",\"inputDir\":\"/tmp/jobs/2026-01-17\",\"runsDir\":\"/tmp/runs\",\"exportsDir\":\"/tmp/exports\"}"
                    )
            )
    )
    @PostMapping("/start")
    public ResponseEntity<RunResponse> startRun(@Valid @RequestBody StartRunRequest request) {
        logger.info("Start run request: date={} inputDir={} runsDir={} exportsDir={}",
                request.runDate(), request.inputDir(), request.runsDir(), request.exportsDir());
        RunResponse response = runService.startRun(request);
        logger.info("Start run response: id={} status={}", response.id(), response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reprocess existing extractions")
    @ApiResponse(responseCode = "200", description = "Run created",
            content = @Content(schema = @Schema(implementation = RunResponse.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "ReprocessRequest",
                            value = "{\"runDate\":\"2026-01-17\",\"runsDir\":\"/tmp/runs\",\"exportsDir\":\"/tmp/exports\"}"
                    )
            )
    )
    @PostMapping("/reprocess")
    public ResponseEntity<RunResponse> reprocess(@Valid @RequestBody ReprocessRequest request) {
        logger.info("Reprocess request: date={} runsDir={} exportsDir={}",
                request.runDate(), request.runsDir(), request.exportsDir());
        RunResponse response = runService.reprocess(request);
        logger.info("Reprocess response: id={} status={}", response.id(), response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Apply review edits")
    @ApiResponse(responseCode = "200", description = "Run created",
            content = @Content(schema = @Schema(implementation = RunResponse.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "ApplyReviewRequest",
                            value = "{\"runDate\":\"2026-01-17\",\"reviewCsvPath\":\"/tmp/exports/2026-01-17/review_queue.csv\",\"exportsDir\":\"/tmp/exports\"}"
                    )
            )
    )
    @PostMapping("/apply-review")
    public ResponseEntity<RunResponse> applyReview(@Valid @RequestBody ApplyReviewRequest request) {
        logger.info("Apply review request: date={} reviewCsvPath={} exportsDir={}",
                request.runDate(), request.reviewCsvPath(), request.exportsDir());
        RunResponse response = runService.applyReview(request);
        logger.info("Apply review response: id={} status={}", response.id(), response.status());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get run by id")
    @ApiResponse(responseCode = "200", description = "Run details",
            content = @Content(schema = @Schema(implementation = RunResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<RunResponse> getRun(@PathVariable Long id) {
        logger.info("Get run request: id={}", id);
        return runService.getRun(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "List runs")
    @ApiResponse(responseCode = "200", description = "Run list")
    @GetMapping
    public ResponseEntity<List<RunSummary>> listRuns() {
        List<RunSummary> runs = runService.listRuns();
        logger.info("List runs response: count={}", runs.size());
        return ResponseEntity.ok(runs);
    }
}
