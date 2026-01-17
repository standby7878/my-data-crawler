package com.mycrawler.orchestrator.dto;

import com.mycrawler.orchestrator.db.RunStatus;
import com.mycrawler.orchestrator.db.RunType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Run status response")
public record RunResponse(
        @Schema(description = "Run id")
        Long id,
        @Schema(description = "Run type")
        RunType runType,
        @Schema(description = "Run status")
        RunStatus status,
        @Schema(description = "Run date")
        String runDate,
        @Schema(description = "Input directory")
        String inputDir,
        @Schema(description = "Runs directory")
        String runsDir,
        @Schema(description = "Exports directory")
        String exportsDir,
        @Schema(description = "Review CSV path")
        String reviewCsvPath,
        @Schema(description = "Created at")
        Instant createdAt,
        @Schema(description = "Started at")
        Instant startedAt,
        @Schema(description = "Finished at")
        Instant finishedAt,
        @Schema(description = "Message")
        String message
) {
}
