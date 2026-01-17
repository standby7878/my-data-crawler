package com.mycrawler.orchestrator.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "runs")
public class RunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunType runType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(nullable = false)
    private String runDate;

    @Column(nullable = false)
    private String inputDir;

    @Column(nullable = false)
    private String runsDir;

    @Column(nullable = false)
    private String exportsDir;

    @Column
    private String reviewCsvPath;

    @Column
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    public Long getId() {
        return id;
    }

    public RunType getRunType() {
        return runType;
    }

    public void setRunType(RunType runType) {
        this.runType = runType;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getRunsDir() {
        return runsDir;
    }

    public void setRunsDir(String runsDir) {
        this.runsDir = runsDir;
    }

    public String getExportsDir() {
        return exportsDir;
    }

    public void setExportsDir(String exportsDir) {
        this.exportsDir = exportsDir;
    }

    public String getReviewCsvPath() {
        return reviewCsvPath;
    }

    public void setReviewCsvPath(String reviewCsvPath) {
        this.reviewCsvPath = reviewCsvPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
