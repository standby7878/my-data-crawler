package com.mycrawler.orchestrator.controller;

import com.mycrawler.orchestrator.dto.SearchRequest;
import com.mycrawler.orchestrator.dto.SearchResponse;
import com.mycrawler.orchestrator.service.SearxngService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Search", description = "SearXNG search API")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final SearxngService searxngService;

    public SearchController(SearxngService searxngService) {
        this.searxngService = searxngService;
    }

    @Operation(summary = "Perform generic web search")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "SearchRequest",
                            value = "{\"query\":\"site:duunitori.fi kesatyo\",\"max_results\":10,\"categories\":[\"general\"],\"engines\":[\"google\",\"duckduckgo\"]}"
                    )
            )
    )
    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        logger.info("Search request: query={} maxResults={} categories={} engines={}",
                request.query(), request.maxResults(), request.categories(), request.engines());
        SearchResponse response = searxngService.search(request);
        logger.info("Search response: query={} totalResults={}", response.query(), response.totalResults());
        return response;
    }
}
