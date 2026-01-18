package com.mycrawler.orchestrator.controller;

import com.mycrawler.orchestrator.dto.E2eSearchJobsCrawlRequest;
import com.mycrawler.orchestrator.dto.E2eSearchJobsCrawlResponse;
import com.mycrawler.orchestrator.service.E2eSearchJobsCrawlService;
import com.mycrawler.orchestrator.service.SearxngService;
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
@RequestMapping("/api/v1/e2e")
@Tag(name = "E2E", description = "Manual end-to-end test endpoints")
public class E2eController {
    private static final Logger logger = LoggerFactory.getLogger(E2eController.class);
    private final E2eSearchJobsCrawlService e2eSearchJobsCrawlService;

    public E2eController(E2eSearchJobsCrawlService e2eSearchJobsCrawlService) {
        this.e2eSearchJobsCrawlService = e2eSearchJobsCrawlService;
    }

    @Operation(summary = "Search jobs via SearXNG and run the Python crawler over returned URLs")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "E2eSearchJobsCrawlRequest",
                            value = "{\"site\":\"all\",\"query\":\"kesätyö opiskelijat Uusimaa\",\"max_results\":3}"
                    )
            )
    )
    @PostMapping("/search-jobs-crawl")
    public E2eSearchJobsCrawlResponse searchJobsCrawl(@Valid @RequestBody E2eSearchJobsCrawlRequest request, HttpServletRequest httpRequest) {
        logger.info("E2E search-jobs-crawl request: site={} maxResults={} date={}", request.site(), request.maxResults(), request.date());
        E2eSearchJobsCrawlResponse response = e2eSearchJobsCrawlService.run(request, SearxngService.ForwardedHeaders.from(httpRequest));
        logger.info("E2E search-jobs-crawl response: urls={} tmpDir={} exitCode={}", response.urls().size(), response.tmpDir(), response.crawlerExitCode());
        return response;
    }
}

