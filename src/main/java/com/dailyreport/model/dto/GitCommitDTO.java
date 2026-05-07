package com.dailyreport.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GitCommitDTO {

    private String commitId;
    private String message;
    private String author;
    private LocalDateTime commitTime;
    private List<String> changedFiles;

    public GitCommitDTO() {}

    public GitCommitDTO(String commitId, String message, String author,
                        LocalDateTime commitTime, List<String> changedFiles) {
        this.commitId = commitId;
        this.message = message;
        this.author = author;
        this.commitTime = commitTime;
        this.changedFiles = changedFiles;
    }

    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getCommitTime() { return commitTime; }
    public void setCommitTime(LocalDateTime commitTime) { this.commitTime = commitTime; }

    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
}
