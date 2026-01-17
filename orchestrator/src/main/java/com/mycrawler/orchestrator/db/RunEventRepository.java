package com.mycrawler.orchestrator.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunEventRepository extends JpaRepository<RunEvent, Long> {
    List<RunEvent> findByRunIdOrderByCreatedAtAsc(Long runId);
}
