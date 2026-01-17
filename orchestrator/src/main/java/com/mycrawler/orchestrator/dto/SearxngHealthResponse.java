package com.mycrawler.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SearXNG connectivity status")
public record SearxngHealthResponse(
        @Schema(description = "Base URL of the SearXNG instance")
        String baseUrl,
        @Schema(description = "HTTP status code from SearXNG")
        Integer statusCode,
        @Schema(description = "Whether connectivity check succeeded")
        boolean ok,
        @Schema(description = "Optional message")
        String message
) {
}
