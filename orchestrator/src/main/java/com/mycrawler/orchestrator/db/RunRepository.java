package com.mycrawler.orchestrator.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RunRepository extends JpaRepository<RunEntity, Long> {
}
