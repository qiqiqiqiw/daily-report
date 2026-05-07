package com.dailyreport.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_reports")
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Lob
    @Column(name = "raw_commits", columnDefinition = "CLOB")
    private String rawCommits;

    @Lob
    @Column(name = "completed_tasks", columnDefinition = "CLOB")
    private String completedTasks;

    @Lob
    @Column(name = "in_progress_tasks", columnDefinition = "CLOB")
    private String inProgressTasks;

    @Lob
    @Column(name = "notes", columnDefinition = "CLOB")
    private String notes;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }

    public String getRawCommits() { return rawCommits; }
    public void setRawCommits(String rawCommits) { this.rawCommits = rawCommits; }

    public String getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(String completedTasks) { this.completedTasks = completedTasks; }

    public String getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(String inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public void setEdited(Boolean edited) { isEdited = edited; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
