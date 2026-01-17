package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to start a full extraction run")
public record StartRunRequest(
        @Schema(description = "Run date in YYYY-MM-DD")
        @NotBlank String runDate,
        @Schema(description = "Input directory with jobs/YYYY-MM-DD")
        @NotBlank String inputDir,
        @Schema(description = "Runs output directory")
        @NotBlank String runsDir,
        @Schema(description = "Exports output directory")
        @NotBlank String exportsDir
) {
}
