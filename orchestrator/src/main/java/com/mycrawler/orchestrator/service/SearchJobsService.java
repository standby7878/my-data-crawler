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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchJobsService {
    private static final List<String> SITE_LOOP_ORDER = List.of("mol.fi", "duunitori.fi", "oikotie.fi", "te-palvelut.fi");
    private static final String DEFAULT_QUERY = "kesätyö opiskelijat Uusimaa";
    private static final int DEFAULT_MAX_RESULTS = 50;
    private static final int DEFAULT_VARIANTS_PER_SITE = 6;

    private final SearxngService searxngService;
    private final List<String> primaryEngines;
    private final List<String> fallbackEngines;

    public SearchJobsService(
            SearxngService searxngService,
            @Value("${searxng.engines.primary:google,bing,brave}") String primaryEngines,
            @Value("${searxng.engines.fallback:qwant,mojeek}") String fallbackEngines
    ) {
        this.searxngService = searxngService;
        this.primaryEngines = parseCsv(primaryEngines);
        this.fallbackEngines = parseCsv(fallbackEngines);
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
            List<String> variants = buildQueryVariants(site, queryValue, DEFAULT_VARIANTS_PER_SITE);
            remaining = runQueries(site, variants, remaining, primaryEngines, forwardedHeaders, merged, seen, totalLimit);
            if (remaining > 0 && !fallbackEngines.isEmpty()) {
                runQueries(site, variants, remaining, fallbackEngines, forwardedHeaders, merged, seen, totalLimit);
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

    static List<String> buildQueryVariants(String site, String queryValue, int maxVariants) {
        String base = queryValue == null ? "" : queryValue.trim();
        if (base.isEmpty()) {
            base = DEFAULT_QUERY;
        }

        String lang = SearxngService.detectLang(base);
        List<String> rawVariants = "fi".equalsIgnoreCase(lang)
                ? buildFinnishVariants(base, maxVariants)
                : buildEnglishVariants(base, maxVariants);

        Set<String> deduped = new LinkedHashSet<>();
        for (String variant : rawVariants) {
            if (variant == null || variant.isBlank()) {
                continue;
            }
            String trimmed = variant.trim().replaceAll("\\s+", " ");
            String withSite = ensureStrictSitePrefix(site, trimmed);
            deduped.add(withSite);
            if (deduped.size() >= maxVariants) {
                break;
            }
        }
        return List.copyOf(deduped);
    }

    private static List<String> buildFinnishVariants(String base, int maxVariants) {
        List<String> variants = new ArrayList<>();
        variants.add(base);
        List<String> locations = List.of("Uusimaa", "Helsinki", "Espoo", "Vantaa");
        List<List<String>> combos = List.of(
                List.of("kesätyö", "opiskelija"),
                List.of("kesäduuni", "opiskelija"),
                List.of("kesäharjoittelu", "opiskelija"),
                List.of("kesätyö", "harjoittelija"),
                List.of("kesätyö", "trainee"),
                List.of("kesätyö", "opiskelijoille")
        );
        int idx = 0;
        while (variants.size() < maxVariants && idx < combos.size() * locations.size()) {
            List<String> combo = combos.get(idx % combos.size());
            String loc = locations.get(idx % locations.size());
            variants.add(String.join(" ", combo) + " " + loc);
            idx += 1;
        }
        return variants;
    }

    private static List<String> buildEnglishVariants(String base, int maxVariants) {
        List<String> variants = new ArrayList<>();
        variants.add(base);
        List<String> locations = List.of("Uusimaa", "Helsinki", "Espoo", "Vantaa");
        List<List<String>> combos = List.of(
                List.of("summer", "job", "student"),
                List.of("summer", "job", "internship"),
                List.of("summer", "trainee", "student"),
                List.of("internship", "student"),
                List.of("summer", "intern", "Helsinki")
        );
        int idx = 0;
        while (variants.size() < maxVariants && idx < combos.size() * locations.size()) {
            List<String> combo = combos.get(idx % combos.size());
            String loc = locations.get(idx % locations.size());
            variants.add(String.join(" ", combo) + " " + loc);
            idx += 1;
        }
        return variants;
    }

    private static String ensureStrictSitePrefix(String site, String query) {
        if (query == null || query.isBlank()) {
            return "site:" + site;
        }
        if (query.matches("(?is).*\\bsite:.*")) {
            return query;
        }
        return "site:" + site + " " + query;
    }

    private int runQueries(
            String site,
            List<String> variants,
            int remaining,
            List<String> engines,
            SearxngService.ForwardedHeaders forwardedHeaders,
            List<SearchJobsResult> merged,
            Map<String, Boolean> seen,
            int totalLimit
    ) {
        if (remaining <= 0 || variants.isEmpty()) {
            return remaining;
        }
        for (String q : variants) {
            if (merged.size() >= totalLimit) {
                break;
            }
            int perQueryLimit = Math.max(1, Math.min(remaining, 25));
            SearchResponse response = searxngService.search(
                    new SearchRequest(q, perQueryLimit, List.of("general"), engines),
                    forwardedHeaders
            );
            remaining = mergeResults(site, response, merged, seen, totalLimit);
            if (remaining <= 0) {
                break;
            }
        }
        return remaining;
    }

    private int mergeResults(
            String site,
            SearchResponse response,
            List<SearchJobsResult> merged,
            Map<String, Boolean> seen,
            int totalLimit
    ) {
        if (response == null || response.results() == null) {
            return Math.max(0, totalLimit - merged.size());
        }
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
        return Math.max(0, totalLimit - merged.size());
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

    private static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }
}
