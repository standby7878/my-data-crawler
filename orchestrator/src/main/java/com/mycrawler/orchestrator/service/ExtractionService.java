package com.mycrawler.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.run.HtmlPreprocessor;
import com.mycrawler.orchestrator.run.JobPosting;
import com.mycrawler.orchestrator.run.MetaData;
import com.mycrawler.orchestrator.run.MetaParser;
import com.mycrawler.orchestrator.run.PreprocessedDocument;
import com.mycrawler.orchestrator.run.SimpleExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);
    private final ObjectMapper objectMapper;
    private final HtmlPreprocessor preprocessor;
    private final SimpleExtractor extractor;
    private final MetaParser metaParser;

    public ExtractionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.preprocessor = new HtmlPreprocessor();
        this.extractor = new SimpleExtractor();
        this.metaParser = new MetaParser(objectMapper);
    }

    public JobPosting extractFromHtml(Path htmlPath) throws IOException {
        logger.debug("Extracting from {}", htmlPath);
        PreprocessedDocument doc = preprocessor.preprocess(htmlPath);
        MetaData meta = metaParser.parse(resolveMetaPath(htmlPath));
        return extractor.extract(doc, meta);
    }

    public void writeRawExtraction(Path outputPath, JobPosting posting) throws IOException {
        Files.createDirectories(outputPath.getParent());
        objectMapper.writeValue(outputPath.toFile(), posting);
        logger.debug("Wrote raw extraction {}", outputPath);
    }

    private Path resolveMetaPath(Path htmlPath) {
        String filename = htmlPath.getFileName().toString();
        String metaName = filename.replace(".html", "_meta.json");
        return htmlPath.getParent().resolve(metaName);
    }
}
