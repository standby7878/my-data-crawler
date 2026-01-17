package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Search request")
public record SearchRequest(
        @Schema(description = "Search query string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String query,
        @Schema(description = "Maximum results to return", defaultValue = "50")
        @JsonProperty("max_results")
        @Min(1) @Max(200) Integer maxResults,
    @Schema(description = "Search categories")
    List<String> categories,
    @Schema(description = "Search engines to use (e.g., google, duckduckgo, bing)")
    List<String> engines
) {
}
