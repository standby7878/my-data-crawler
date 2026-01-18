package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Merged job search response")
public record SearchJobsResponse(
        @Schema(description = "Requested site selector value")
        @JsonProperty("requested_site")
        String requestedSite,
        @Schema(description = "Sites searched in order")
        @JsonProperty("sites_searched")
        List<String> sitesSearched,
        @Schema(description = "Total results returned (deduplicated)")
        @JsonProperty("total_deduped")
        int totalDeduped,
        @Schema(description = "Query value used")
        String query,
        @Schema(description = "Merged result list")
        List<SearchJobsResult> results,
        @Schema(description = "Response timestamp")
        String timestamp
) {
}

