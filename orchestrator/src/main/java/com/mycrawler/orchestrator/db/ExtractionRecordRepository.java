package com.mycrawler.orchestrator.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionRecordRepository extends JpaRepository<ExtractionRecord, Long> {
    List<ExtractionRecord> findByRunIdOrderByCreatedAtAsc(Long runId);
}
