package com.dailyreport.model.dto;

public class LLMReportResult {

    private String completedTasks;
    private String inProgressTasks;
    private String notes;

    public LLMReportResult() {}

    public LLMReportResult(String completedTasks, String inProgressTasks, String notes) {
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.notes = notes;
    }

    public String getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(String completedTasks) { this.completedTasks = completedTasks; }

    public String getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(String inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
