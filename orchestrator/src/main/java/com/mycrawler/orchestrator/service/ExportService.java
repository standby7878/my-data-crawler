package com.mycrawler.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycrawler.orchestrator.run.JobPosting;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private final ObjectMapper objectMapper;

    public ExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeExports(Path exportsDir, String runDate, List<JobPosting> postings) throws IOException {
        Path exportDir = exportsDir.resolve(runDate);
        Files.createDirectories(exportDir);
        writeJsonl(exportDir.resolve("jobs.jsonl"), postings);
        writeCsv(exportDir.resolve("jobs.csv"), postings);
        logger.info("Exported {} postings to {}", postings.size(), exportDir);
    }

    private void writeJsonl(Path path, List<JobPosting> postings) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (JobPosting posting : postings) {
                writer.write(objectMapper.writeValueAsString(posting));
                writer.newLine();
            }
        }
    }

    private void writeCsv(Path path, List<JobPosting> postings) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("job_id", "job_title", "company_name", "location_municipality", "source_url", "confidence")
                .build();
        try (BufferedWriter writer = Files.newBufferedWriter(path);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (JobPosting posting : postings) {
                printer.printRecord(
                        posting.getJobId(),
                        posting.getJobTitle(),
                        posting.getCompanyName(),
                        posting.getLocationMunicipality(),
                        posting.getSourceUrl(),
                        posting.getExtractionConfidence());
            }
        }
    }
}
