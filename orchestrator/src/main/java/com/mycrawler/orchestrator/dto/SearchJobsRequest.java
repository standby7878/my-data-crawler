package com.mycrawler.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Search jobs request (searches selected site(s) and merges results)")
public record SearchJobsRequest(
        @Schema(
                description = "Site selector",
                allowableValues = {"all", "mol.fi", "duunitori.fi", "oikotie.fi", "te-palvelut.fi"},
                defaultValue = "all"
        )
        JobSearchSite site,
        @Schema(description = "Query value (site prefix is applied automatically)", defaultValue = "kesätyö opiskelijat Uusimaa")
        String query,
        @Schema(description = "Total max results across all sites", defaultValue = "50")
        @JsonProperty("max_results")
        @Min(1) @Max(200) Integer maxResults
) {
}

