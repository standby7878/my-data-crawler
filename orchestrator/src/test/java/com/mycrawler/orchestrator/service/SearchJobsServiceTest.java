package com.mycrawler.orchestrator.service;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchJobsServiceTest {

    @Test
    void buildQueryVariantsAlwaysPrefixesStrictSiteOperator() {
        List<String> variants = SearchJobsService.buildQueryVariants("duunitori.fi", "kesätyö opiskelija Uusimaa", 6);
        assertFalse(variants.isEmpty());
        assertTrue(variants.size() <= 6);
        for (String variant : variants) {
            assertTrue(variant.startsWith("site:duunitori.fi "));
            assertFalse(variant.startsWith("site duunitori.fi "));
        }
    }
}

