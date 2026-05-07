package com.dailyreport.model.dto;

import java.time.LocalDate;

public class ReportGenerateRequest {

    private LocalDate date;
    private Long repositoryId;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
}
