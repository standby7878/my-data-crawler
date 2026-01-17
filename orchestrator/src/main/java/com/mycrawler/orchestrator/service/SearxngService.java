package com.mycrawler.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.dto.SearchRequest;
import com.mycrawler.orchestrator.dto.SearchResponse;
import com.mycrawler.orchestrator.dto.SearchResult;
import com.mycrawler.orchestrator.dto.SearxngHealthResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SearxngService {
    private static final Logger logger = LoggerFactory.getLogger(SearxngService.class);
    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public SearxngService(
            RestClient.Builder restClientBuilder,
            @Value("${searxng.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    public SearxngHealthResponse checkHealth() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("q", "test")
                .queryParam("format", "json")
                .toUriString();
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class);
            logger.info("SearXNG health check response status={}", response.getStatusCode().value());
            return new SearxngHealthResponse(baseUrl, response.getStatusCode().value(), response.getStatusCode().is2xxSuccessful(), null);
        } catch (RestClientException ex) {
            logger.warn("SearXNG health check failed: {}", ex.getMessage());
            return new SearxngHealthResponse(baseUrl, null, false, ex.getMessage());
        }
    }

    public SearchResponse search(SearchRequest request) {
        int maxResults = Optional.ofNullable(request.maxResults()).orElse(50);
        List<SearchResult> results = new ArrayList<>();
        int page = 1;
        int maxPages = 10;
        logger.info("SearXNG search start: query={} maxResults={}", request.query(), maxResults);
        while (results.size() < maxResults && page <= maxPages) {
            JsonNode root = fetchPage(request, page);
            JsonNode items = root.path("results");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                results.add(new SearchResult(
                        item.path("url").asText(null),
                        item.path("title").asText(null),
                        item.path("content").asText(null),
                        item.path("engine").asText(null),
                        item.path("score").isNumber() ? item.path("score").asDouble() : null,
                        item.path("thumbnail").asText(null),
                        item.path("publishedDate").asText(null)
                ));
                if (results.size() >= maxResults) {
                    break;
                }
            }
            if (items.size() < 20) {
                break;
            }
            page += 1;
        }
        logger.info("SearXNG search done: query={} totalResults={}", request.query(), results.size());
        return new SearchResponse(
                request.query(),
                results.size(),
                results,
                Instant.now().toString()
        );
    }

    private JsonNode fetchPage(SearchRequest request, int page) {
        String normalizedQuery = normalizeQuery(request.query());
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("q", normalizedQuery)
                .queryParam("format", "json")
                .queryParam("lang", "en")
                .queryParam("pageno", page)
                .queryParamIfPresent("categories", joinParam(request.categories()))
                .toUriString();
        try {
            String body = restClient.get()
                    .uri(url)
                    .headers(headers -> {
                        headers.add("User-Agent", "JobOrchestrator/0.1 SearXNG-Client");
                        headers.add("Accept", "application/json");
                        headers.add("Accept-Language", "en-US,en;q=0.5");
                        headers.add("Accept-Encoding", "gzip, deflate");
                        headers.add("Connection", "keep-alive");
                        headers.add("X-Forwarded-For", "127.0.0.1");
                        headers.add("X-Real-IP", "127.0.0.1");
                        headers.add("Referer", baseUrl);
                    })
                    .exchange((req, res) -> {
                        int status = res.getStatusCode().value();
                        byte[] bytes = res.getBody().readAllBytes();
                        String responseBody = bytes.length == 0 ? "" : new String(bytes);
                        if (status < 200 || status >= 300) {
                            logger.warn("SearXNG returned status {} for {}", status, url);
                            return "";
                        }
                        return responseBody;
                    });
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode().putArray("results");
            }
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            logger.warn("SearXNG request failed: {}", ex.getMessage());
            return objectMapper.createObjectNode().putArray("results");
        }
    }

    private Optional<String> joinParam(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(",", values));
    }

    private String normalizeQuery(String query) {
        String raw = query == null ? "" : query.trim();
        if (raw.isEmpty()) {
            return "Uusimaa";
        }

        String withoutColons = raw.replace(':', ' ');
        String collapsed = withoutColons.trim().replaceAll("\\s+", " ");
        if (collapsed.isEmpty()) {
            return "Uusimaa";
        }
        if (collapsed.matches("(?is).*\\buusimaa\\b.*")) {
            return collapsed;
        }
        return collapsed + " Uusimaa";
    }
}
