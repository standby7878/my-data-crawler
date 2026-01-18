package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Job search result item with site origin")
public record SearchJobsResult(
        @Schema(description = "Site that produced this result (input site loop)")
        String site,
        @Schema(description = "Result URL")
        String url,
        @Schema(description = "Result title")
        String title,
        @Schema(description = "Result content snippet")
        String content,
        @Schema(description = "Search engine name")
        String engine,
        @Schema(description = "Result score")
        Double score,
        @Schema(description = "Thumbnail URL")
        String thumbnail,
        @Schema(description = "Published date")
        String publishedDate
) {
}

