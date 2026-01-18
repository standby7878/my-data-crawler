package com.mycrawler.orchestrator.controller;

import com.mycrawler.orchestrator.dto.SearchJobsRequest;
import com.mycrawler.orchestrator.dto.SearchJobsResponse;
import com.mycrawler.orchestrator.service.SearxngService;
import com.mycrawler.orchestrator.service.SearchJobsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
public class SearchJobsController {
    private static final Logger logger = LoggerFactory.getLogger(SearchJobsController.class);
    private final SearchJobsService searchJobsService;

    public SearchJobsController(SearchJobsService searchJobsService) {
        this.searchJobsService = searchJobsService;
    }

    @Operation(summary = "Search jobs across one site or all sites (merged & deduplicated)")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "SearchJobsRequest",
                            value = "{\"site\":\"all\",\"query\":\"kesätyö opiskelijat Uusimaa\",\"max_results\":50}"
                    )
            )
    )
    @PostMapping("/search-jobs")
    public SearchJobsResponse searchJobs(@Valid @RequestBody SearchJobsRequest request, HttpServletRequest httpRequest) {
        logger.info("Search-jobs request: site={} maxResults={}", request.site(), request.maxResults());
        SearchJobsResponse response = searchJobsService.searchJobs(request, SearxngService.ForwardedHeaders.from(httpRequest));
        logger.info("Search-jobs response: requestedSite={} totalDeduped={}", response.requestedSite(), response.totalDeduped());
        return response;
    }
}

