package com.mycrawler.orchestrator.dto;

import com.mycrawler.orchestrator.db.RunStatus;
import com.mycrawler.orchestrator.db.RunType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Run summary")
public record RunSummary(
        @Schema(description = "Run id")
        Long id,
        @Schema(description = "Run type")
        RunType runType,
        @Schema(description = "Run status")
        RunStatus status,
        @Schema(description = "Run date")
        String runDate,
        @Schema(description = "Created at")
        Instant createdAt,
        @Schema(description = "Finished at")
        Instant finishedAt
) {
}
