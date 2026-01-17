package com.mycrawler.orchestrator.controller;

import com.mycrawler.orchestrator.dto.SearxngHealthResponse;
import com.mycrawler.orchestrator.service.SearxngService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/searxng")
@Tag(name = "SearXNG", description = "SearXNG connectivity checks")
public class SearxngController {
    private static final Logger logger = LoggerFactory.getLogger(SearxngController.class);
    private final SearxngService searxngService;

    public SearxngController(SearxngService searxngService) {
        this.searxngService = searxngService;
    }

    @Operation(summary = "Check SearXNG connectivity")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Connectivity status",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "SearxngHealthResponse",
                            value = "{\"baseUrl\":\"http://localhost:8080\",\"statusCode\":200,\"ok\":true,\"message\":null}"
                    )
            )
    )
    @GetMapping("/health")
    public SearxngHealthResponse health() {
        SearxngHealthResponse response = searxngService.checkHealth();
        logger.info("SearXNG health: baseUrl={} ok={} statusCode={}",
                response.baseUrl(), response.ok(), response.statusCode());
        return response;
    }

    // Search endpoint is exposed at /api/v1/search to match bc-webspider.
}
