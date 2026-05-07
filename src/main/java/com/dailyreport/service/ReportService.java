package com.dailyreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dailyreport.model.AppSettings;
import com.dailyreport.model.DailyReport;
import com.dailyreport.model.GitRepository;
import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.model.dto.LLMReportResult;
import com.dailyreport.repository.AppSettingsRepository;
import com.dailyreport.repository.DailyReportRepository;
import com.dailyreport.repository.GitRepositoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final DailyReportRepository reportRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final AppSettingsRepository settingsRepository;
    private final GitService gitService;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    public ReportService(DailyReportRepository reportRepository,
                         GitRepositoryRepository gitRepositoryRepository,
                         AppSettingsRepository settingsRepository,
                         GitService gitService,
                         LLMService llmService) {
        this.reportRepository = reportRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.settingsRepository = settingsRepository;
        this.gitService = gitService;
        this.llmService = llmService;
        this.objectMapper = new ObjectMapper();
    }

    public DailyReport generateAndSave(LocalDate date, Long repositoryId) {
        GitRepository repo = gitRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("仓库不存在: " + repositoryId));

        // 检查是否已有编辑过的日报
        Optional<DailyReport> existing = reportRepository.findByReportDateAndRepositoryId(date, repositoryId);
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getIsEdited())) {
            throw new RuntimeException("该日期已有手动编辑的日报，请先删除后再重新生成");
        }

        List<GitCommitDTO> commits = gitService.getCommits(repo.getLocalPath(), date);

        String rawCommitsJson;
        try {
            rawCommitsJson = objectMapper.writeValueAsString(commits);
        } catch (Exception e) {
            rawCommitsJson = "[]";
        }

        LLMReportResult llmResult;
        if (commits.isEmpty()) {
            llmResult = new LLMReportResult("（当日无提交记录）", "", "");
        } else {
            llmResult = llmService.generateDailyReport(commits);
        }

        DailyReport report = existing.orElse(new DailyReport());
        report.setReportDate(date);
        report.setRepositoryId(repositoryId);
        report.setRawCommits(rawCommitsJson);
        report.setCompletedTasks(llmResult.getCompletedTasks());
        report.setInProgressTasks(llmResult.getInProgressTasks());
        report.setNotes(llmResult.getNotes());
        report.setIsEdited(false);

        return reportRepository.save(report);
    }

    public List<DailyReport> getMonthlyReports(int year, int month, Long repositoryId) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        return reportRepository.findByRepositoryIdAndDateRange(repositoryId, startDate, endDate);
    }

    public DailyReport getByDate(LocalDate date, Long repositoryId) {
        return reportRepository.findByReportDateAndRepositoryId(date, repositoryId)
                .orElse(null);
    }

    public DailyReport createReport(LocalDate date, Long repositoryId, DailyReport data) {
        Long rid = repositoryId != null ? repositoryId : 0L;
        Optional<DailyReport> existing = reportRepository.findByReportDateAndRepositoryId(date, rid);
        if (existing.isPresent()) {
            return updateReport(existing.get().getId(), data);
        }
        DailyReport report = new DailyReport();
        report.setReportDate(date);
        report.setRepositoryId(rid);
        report.setCompletedTasks(data.getCompletedTasks());
        report.setInProgressTasks(data.getInProgressTasks());
        report.setNotes(data.getNotes());
        report.setIsEdited(true);
        return reportRepository.save(report);
    }

    public DailyReport updateReport(Long id, DailyReport updateData) {
        DailyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("日报不存在: " + id));

        if (updateData.getCompletedTasks() != null) {
            report.setCompletedTasks(updateData.getCompletedTasks());
        }
        if (updateData.getInProgressTasks() != null) {
            report.setInProgressTasks(updateData.getInProgressTasks());
        }
        if (updateData.getNotes() != null) {
            report.setNotes(updateData.getNotes());
        }
        report.setIsEdited(true);

        return reportRepository.save(report);
    }

    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
    }

    @Scheduled(cron = "${daily-report.cron}")
    public void autoGenerate() {
        Optional<AppSettings> enabledSetting = settingsRepository.findBySettingKey("auto_generate_enabled");
        if (enabledSetting.isEmpty() || !"true".equals(enabledSetting.get().getSettingValue())) {
            return;
        }

        Optional<AppSettings> repoSetting = settingsRepository.findBySettingKey("default_repository_id");
        if (repoSetting.isEmpty() || repoSetting.get().getSettingValue() == null) {
            return;
        }

        try {
            Long repoId = Long.parseLong(repoSetting.get().getSettingValue());
            LocalDate today = LocalDate.now();
            generateAndSave(today, repoId);
        } catch (Exception e) {
            // 定时任务失败时不阻塞，记录错误即可
            System.err.println("自动生成日报失败: " + e.getMessage());
        }
    }
}
