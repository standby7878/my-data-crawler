package com.mycrawler.orchestrator.service;

import com.mycrawler.orchestrator.dto.JobSearchSite;
import com.mycrawler.orchestrator.dto.SearchJobsRequest;
import com.mycrawler.orchestrator.dto.SearchJobsResponse;
import com.mycrawler.orchestrator.dto.SearchJobsResult;
import com.mycrawler.orchestrator.dto.SearchRequest;
import com.mycrawler.orchestrator.dto.SearchResponse;
import com.mycrawler.orchestrator.dto.SearchResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SearchJobsService {
    private static final List<String> SITE_LOOP_ORDER = List.of("mol.fi", "duunitori.fi", "oikotie.fi", "te-palvelut.fi");
    private static final String DEFAULT_QUERY = "kesätyö opiskelijat Uusimaa";
    private static final int DEFAULT_MAX_RESULTS = 50;

    private final SearxngService searxngService;

    public SearchJobsService(SearxngService searxngService) {
        this.searxngService = searxngService;
    }

    public SearchJobsResponse searchJobs(SearchJobsRequest request, SearxngService.ForwardedHeaders forwardedHeaders) {
        JobSearchSite requestedSite = request != null ? request.site() : null;
        String requestedSiteValue = requestedSite == null ? JobSearchSite.ALL.value() : requestedSite.value();
        String queryValue = request != null && request.query() != null && !request.query().isBlank()
                ? request.query().trim()
                : DEFAULT_QUERY;
        int totalLimit = request != null && request.maxResults() != null ? request.maxResults() : DEFAULT_MAX_RESULTS;

        List<String> sitesSearched = resolveSitesToSearch(requestedSite);
        List<SearchJobsResult> merged = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String site : sitesSearched) {
            if (merged.size() >= totalLimit) {
                break;
            }
            int remaining = totalLimit - merged.size();
            String siteQuery = buildSiteQuery(site, queryValue);
            SearchResponse response = searxngService.search(
                    new SearchRequest(siteQuery, remaining, List.of("general"), null),
                    forwardedHeaders
            );
            for (SearchResult item : response.results()) {
                if (merged.size() >= totalLimit) {
                    break;
                }
                if (item == null || item.url() == null || item.url().isBlank()) {
                    continue;
                }
                String key = normalizeUrlKey(item.url());
                if (key == null || seen.containsKey(key)) {
                    continue;
                }
                seen.put(key, Boolean.TRUE);
                merged.add(new SearchJobsResult(
                        site,
                        item.url(),
                        item.title(),
                        item.content(),
                        item.engine(),
                        item.score(),
                        item.thumbnail(),
                        item.publishedDate()
                ));
            }
        }

        return new SearchJobsResponse(
                requestedSiteValue,
                sitesSearched,
                merged.size(),
                queryValue,
                merged,
                Instant.now().toString()
        );
    }

    public List<String> extractUrls(SearchJobsResponse response) {
        if (response == null || response.results() == null) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (SearchJobsResult result : response.results()) {
            if (result == null || result.url() == null || result.url().isBlank()) {
                continue;
            }
            String key = normalizeUrlKey(result.url());
            if (key == null || seen.containsKey(key)) {
                continue;
            }
            seen.put(key, Boolean.TRUE);
            urls.add(result.url());
        }
        return urls;
    }

    private List<String> resolveSitesToSearch(JobSearchSite requestedSite) {
        if (requestedSite == null || requestedSite == JobSearchSite.ALL) {
            return SITE_LOOP_ORDER;
        }
        return List.of(requestedSite.value());
    }

    private String buildSiteQuery(String site, String queryValue) {
        return "site " + site + " " + queryValue;
    }

    private String normalizeUrlKey(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int hashIndex = trimmed.indexOf('#');
        String noFragment = hashIndex >= 0 ? trimmed.substring(0, hashIndex) : trimmed;
        String withoutTrailingSlash = noFragment.endsWith("/") && noFragment.length() > 1
                ? noFragment.substring(0, noFragment.length() - 1)
                : noFragment;
        String lowered = withoutTrailingSlash.toLowerCase(Locale.ROOT);
        return Objects.equals(lowered, "") ? null : lowered;
    }
}

