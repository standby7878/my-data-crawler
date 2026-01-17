package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Search response")
public record SearchResponse(
        @Schema(description = "Search query")
        String query,
        @Schema(description = "Total results returned")
        @JsonProperty("total_results")
        int totalResults,
        @Schema(description = "Result list")
        List<SearchResult> results,
        @Schema(description = "Response timestamp")
        String timestamp
) {
}
