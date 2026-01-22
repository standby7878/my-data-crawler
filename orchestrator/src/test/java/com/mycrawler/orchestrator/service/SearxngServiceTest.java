package com.mycrawler.orchestrator.service;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearxngServiceTest {

    @Test
    void normalizeQueryPreservesSiteOperatorAndDoesNotStripColons() {
        String normalized = SearxngService.normalizeQuery("site:duunitori.fi kesätyö");
        assertTrue(normalized.contains("site:duunitori.fi"));
        assertFalse(normalized.contains("site duunitori.fi"));
        assertTrue(normalized.contains(":"));
    }

    @Test
    void normalizeQueryDoesNotAppendUusimaaIfSpecificLocationPresent() {
        assertEquals("kesätyö Helsinki", SearxngService.normalizeQuery("kesätyö Helsinki"));
        assertEquals("summer job Espoo", SearxngService.normalizeQuery("summer job Espoo"));
    }

    @Test
    void detectLangDefaultsToFinnish() {
        assertEquals("fi", SearxngService.detectLang(null));
        assertEquals("fi", SearxngService.detectLang(""));
        assertEquals("fi", SearxngService.detectLang("kesätyö opiskelija Uusimaa"));
        assertEquals("en", SearxngService.detectLang("summer job student Helsinki"));
    }

    @Test
    void buildSearchUrlIncludesEnginesWhenProvided() {
        String url = SearxngService.buildSearchUrl(
                "http://localhost:8080",
                "site:duunitori.fi kesätyö",
                "fi",
                2,
                List.of("general"),
                List.of("google", "bing")
        );
        assertTrue(url.contains("/search"));
        assertTrue(url.contains("q=site"));
        assertTrue(url.contains("duunitori.fi"));
        assertTrue(url.contains("lang=fi"));
        assertTrue(url.contains("pageno=2"));
        assertTrue(url.contains("categories=general"));
        assertTrue(url.contains("engines=google"));
        assertTrue(url.contains("bing"));
    }
}
