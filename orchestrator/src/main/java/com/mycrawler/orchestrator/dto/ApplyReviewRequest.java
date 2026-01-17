package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to apply review edits")
public record ApplyReviewRequest(
        @Schema(description = "Run date in YYYY-MM-DD")
        @NotBlank String runDate,
        @Schema(description = "Review CSV path")
        @NotBlank String reviewCsvPath,
        @Schema(description = "Exports output directory")
        @NotBlank String exportsDir
) {
}
