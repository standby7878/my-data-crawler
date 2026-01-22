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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SearxngService {
    private static final Logger logger = LoggerFactory.getLogger(SearxngService.class);
    private final RestClient restClient;
    private final String baseUrl;
    private final String userAgent;
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_ACCEPT_LANGUAGE = "en-US,en;q=0.5";
    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";

    public SearxngService(
            RestClient.Builder restClientBuilder,
            @Value("${searxng.base-url}") String baseUrl,
            @Value("${searxng.user-agent:Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0}") String userAgent,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.objectMapper = objectMapper;
    }

    public SearxngHealthResponse checkHealth() {
        return checkHealth(null);
    }

    public SearxngHealthResponse checkHealth(ForwardedHeaders forwardedHeaders) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("q", "test")
                .queryParam("format", "json")
                .toUriString();
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .headers(headers -> applyForwardedHeaders(headers, forwardedHeaders))
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
        return search(request, null);
    }

    public SearchResponse search(SearchRequest request, ForwardedHeaders forwardedHeaders) {
        int maxResults = Optional.ofNullable(request.maxResults()).orElse(50);
        List<SearchResult> results = new ArrayList<>();
        int page = 1;
        int maxPages = 10;
        logger.info("SearXNG search start: query={} maxResults={}", request.query(), maxResults);
        while (results.size() < maxResults && page <= maxPages) {
            JsonNode root = fetchPage(request, page, forwardedHeaders);
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

    private JsonNode fetchPage(SearchRequest request, int page, ForwardedHeaders forwardedHeaders) {
        String normalizedQuery = normalizeQuery(request.query());
        String lang = detectLang(request.query());
        String url = buildSearchUrl(
                baseUrl,
                normalizedQuery,
                lang,
                page,
                request.categories(),
                request.engines()
        );
        try {
            String body = restClient.get()
                    .uri(url)
                    .headers(headers -> {
                        applyForwardedHeaders(headers, forwardedHeaders);
                        headers.set(HttpHeaders.ACCEPT, "application/json");
                        headers.set(HttpHeaders.REFERER, baseUrl);
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

    private void applyForwardedHeaders(HttpHeaders headers, ForwardedHeaders forwardedHeaders) {
        String ua = (forwardedHeaders != null && forwardedHeaders.userAgent() != null && !forwardedHeaders.userAgent().isBlank())
                ? forwardedHeaders.userAgent()
                : userAgent;
        headers.set(HttpHeaders.USER_AGENT, ua);

        String acceptLanguage = (forwardedHeaders != null && forwardedHeaders.acceptLanguage() != null && !forwardedHeaders.acceptLanguage().isBlank())
                ? forwardedHeaders.acceptLanguage()
                : DEFAULT_ACCEPT_LANGUAGE;
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);

        String clientIp = (forwardedHeaders != null && forwardedHeaders.clientIp() != null && !forwardedHeaders.clientIp().isBlank())
                ? forwardedHeaders.clientIp()
                : DEFAULT_CLIENT_IP;
        headers.set("X-Forwarded-For", clientIp);
        headers.set("X-Real-IP", clientIp);
    }

    static String normalizeQuery(String query) {
        String raw = query == null ? "" : query.trim();
        if (raw.isEmpty()) {
            return "Uusimaa";
        }

        String collapsed = raw.replaceAll("\\s+", " ").trim();
        if (collapsed.isEmpty()) {
            return "Uusimaa";
        }
        if (containsUusimaaLocation(collapsed)) {
            return collapsed;
        }
        return collapsed + " Uusimaa";
    }

    static String detectLang(String query) {
        String raw = query == null ? "" : query.trim();
        if (raw.isEmpty()) {
            return "fi";
        }
        String lowered = raw.toLowerCase();
        if (lowered.matches(".*[äöå].*")) {
            return "fi";
        }
        if (lowered.matches("(?is).*\\b(kesätyö|kesäduuni|työ|harjoittelu|opiskelija|pääkaupunkiseutu|pk-seutu|uusimaa)\\b.*")) {
            return "fi";
        }
        if (lowered.matches("(?is).*\\b(summer|internship|intern|trainee|student|job)\\b.*")) {
            return "en";
        }
        return "fi";
    }

    static boolean containsUusimaaLocation(String query) {
        if (query == null) {
            return false;
        }
        return query.matches("(?is).*\\b(uusimaa|helsinki|espoo|vantaa|pääkaupunkiseutu|pk-seutu)\\b.*");
    }

    static String buildSearchUrl(
            String baseUrl,
            String query,
            String lang,
            int page,
            List<String> categories,
            List<String> engines
    ) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("lang", lang == null || lang.isBlank() ? "fi" : lang)
                .queryParam("pageno", page)
                .queryParamIfPresent("categories", joinStatic(categories))
                .queryParamIfPresent("engines", joinStatic(engines))
                .toUriString();
    }

    private static Optional<String> joinStatic(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        List<String> filtered = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(",", filtered));
    }

    public record ForwardedHeaders(String userAgent, String acceptLanguage, String clientIp) {
        public static ForwardedHeaders from(HttpServletRequest request) {
            if (request == null) {
                return null;
            }
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String clientIp = resolveClientIp(request);
            return new ForwardedHeaders(userAgent, acceptLanguage, clientIp);
        }

        private static String resolveClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String first = xForwardedFor.split(",")[0].trim();
                if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                    return first;
                }
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp.trim())) {
                return xRealIp.trim();
            }
            return request.getRemoteAddr();
        }
    }
}
