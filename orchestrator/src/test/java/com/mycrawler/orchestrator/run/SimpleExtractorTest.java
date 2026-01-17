package com.mycrawler.orchestrator.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.service.ExtractionService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleExtractorTest {
    @Test
    void extractsBasicFieldsFromHtmlAndMeta() throws Exception {
        Path tempDir = Files.createTempDirectory("extract-test");
        Path htmlPath = tempDir.resolve("sample.html");
        Path metaPath = tempDir.resolve("sample_meta.json");
        Files.writeString(htmlPath, "<html><head><title>Summer Assistant</title></head><body><h1>Summer Assistant</h1><p>Work in Helsinki.</p></body></html>");
        Files.writeString(metaPath, "{\"url\":\"https://example.com/job\",\"company\":\"Example Oy\"}");

        ExtractionService service = new ExtractionService(new ObjectMapper());
        JobPosting posting = service.extractFromHtml(htmlPath);

        assertEquals("Summer Assistant", posting.getJobTitle());
        assertEquals("Example Oy", posting.getCompanyName());
        assertEquals("https://example.com/job", posting.getSourceUrl());
        assertNotNull(posting.getJobDescriptionSummary());
        assertEquals("Uusimaa", posting.getLocationMunicipality());
        assertNotNull(posting.getExtractionConfidence());
    }
}
