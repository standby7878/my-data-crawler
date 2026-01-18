package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "E2E request: search jobs via SearXNG and run the Python crawler over returned URLs")
public record E2eSearchJobsCrawlRequest(
        @Schema(
                description = "Site selector",
                allowableValues = {"all", "mol.fi", "duunitori.fi", "oikotie.fi", "te-palvelut.fi"},
                defaultValue = "all"
        )
        JobSearchSite site,
        @Schema(description = "Query value", defaultValue = "kesätyö opiskelijat Uusimaa")
        String query,
        @Schema(description = "Number of URLs to fetch from SearXNG (total)", defaultValue = "3")
        @JsonProperty("max_results")
        @Min(1) @Max(20) Integer maxResults,
        @Schema(description = "Run date (YYYY-MM-DD). Defaults to today.")
        String date
) {
}

