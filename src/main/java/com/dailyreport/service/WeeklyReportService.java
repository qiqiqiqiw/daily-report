package com.dailyreport.service;

import com.dailyreport.model.CombinedReport;
import com.dailyreport.model.GitRepository;
import com.dailyreport.model.WeeklyReport;
import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.model.dto.LLMReportResult;
import com.dailyreport.repository.CombinedReportRepository;
import com.dailyreport.repository.GitRepositoryRepository;
import com.dailyreport.repository.WeeklyReportRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final CombinedReportRepository combinedReportRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitService gitService;
    private final LLMService llmService;

    public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
                                CombinedReportRepository combinedReportRepository,
                                GitRepositoryRepository gitRepositoryRepository,
                                GitService gitService,
                                LLMService llmService) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.combinedReportRepository = combinedReportRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.gitService = gitService;
        this.llmService = llmService;
    }

    public WeeklyReport generateAndSave(LocalDate anyDateInWeek) {
        LocalDate weekStart = anyDateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<GitRepository> repos = gitRepositoryRepository.findAll();
        if (repos.isEmpty()) {
            throw new RuntimeException("没有已添加的仓库");
        }

        // 按仓库收集一周的提交记录
        Map<String, List<GitCommitDTO>> allCommits = new LinkedHashMap<>();
        for (GitRepository repo : repos) {
            List<GitCommitDTO> weekCommits = new ArrayList<>();
            for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                try {
                    List<GitCommitDTO> dayCommits = gitService.getCommits(repo.getLocalPath(), date);
                    weekCommits.addAll(dayCommits);
                } catch (Exception e) {
                    // 仓库读取失败跳过
                }
            }
            if (!weekCommits.isEmpty()) {
                allCommits.put(repo.getName(), weekCommits);
            }
        }

        if (allCommits.isEmpty()) {
            throw new RuntimeException("本周所有仓库均无提交记录");
        }

        String content = llmService.generateWeeklyReport(allCommits, weekStart, weekEnd);

        Optional<WeeklyReport> existing = weeklyReportRepository.findByWeekStartDate(weekStart);
        WeeklyReport report = existing.orElse(new WeeklyReport());
        report.setWeekStartDate(weekStart);
        report.setWeekEndDate(weekEnd);
        report.setContent(content);
        report.setIsEdited(false);

        return weeklyReportRepository.save(report);
    }

    public WeeklyReport getByWeekStart(LocalDate weekStart) {
        return weeklyReportRepository.findByWeekStartDate(weekStart).orElse(null);
    }

    public WeeklyReport updateReport(Long id, WeeklyReport updateData) {
        WeeklyReport report = weeklyReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("周报不存在: " + id));

        if (updateData.getContent() != null) {
            report.setContent(updateData.getContent());
        }
        report.setIsEdited(true);

        return weeklyReportRepository.save(report);
    }

    public void deleteReport(Long id) {
        weeklyReportRepository.deleteById(id);
    }
}
