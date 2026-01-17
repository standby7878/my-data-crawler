package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to reprocess existing extractions")
public record ReprocessRequest(
        @Schema(description = "Run date in YYYY-MM-DD")
        @NotBlank String runDate,
        @Schema(description = "Runs directory with raw extractions")
        @NotBlank String runsDir,
        @Schema(description = "Exports output directory")
        @NotBlank String exportsDir
) {
}
