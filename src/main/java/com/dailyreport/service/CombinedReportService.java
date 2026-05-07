package com.dailyreport.service;

import com.dailyreport.model.CombinedReport;
import com.dailyreport.model.GitRepository;
import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.model.dto.LLMReportResult;
import com.dailyreport.repository.CombinedReportRepository;
import com.dailyreport.repository.GitRepositoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class CombinedReportService {

    private final CombinedReportRepository combinedReportRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitService gitService;
    private final LLMService llmService;

    public CombinedReportService(CombinedReportRepository combinedReportRepository,
                                  GitRepositoryRepository gitRepositoryRepository,
                                  GitService gitService,
                                  LLMService llmService) {
        this.combinedReportRepository = combinedReportRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.gitService = gitService;
        this.llmService = llmService;
    }

    public CombinedReport generateAndSave(LocalDate date) {
        List<GitRepository> repos = gitRepositoryRepository.findAll();
        if (repos.isEmpty()) {
            throw new RuntimeException("没有已添加的仓库");
        }

        Map<String, List<GitCommitDTO>> allCommits = new LinkedHashMap<>();
        for (GitRepository repo : repos) {
            try {
                List<GitCommitDTO> commits = gitService.getCommits(repo.getLocalPath(), date);
                if (!commits.isEmpty()) {
                    allCommits.put(repo.getName(), commits);
                }
            } catch (Exception e) {
                // 仓库读取失败跳过，不影响其他仓库
            }
        }

        if (allCommits.isEmpty()) {
            throw new RuntimeException("当天所有仓库均无提交记录");
        }

        LLMReportResult llmResult = llmService.generateCombinedReport(allCommits);

        Optional<CombinedReport> existing = combinedReportRepository.findByReportDate(date);
        CombinedReport combined = existing.orElse(new CombinedReport());
        combined.setReportDate(date);
        combined.setCompletedTasks(llmResult.getCompletedTasks());
        combined.setInProgressTasks(llmResult.getInProgressTasks());
        combined.setNotes(llmResult.getNotes());
        combined.setIsEdited(false);

        return combinedReportRepository.save(combined);
    }

    public CombinedReport getByDate(LocalDate date) {
        return combinedReportRepository.findByReportDate(date).orElse(null);
    }

    public List<CombinedReport> getMonthlyReports(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        return combinedReportRepository.findByReportDateBetween(startDate, endDate);
    }

    public CombinedReport updateReport(Long id, CombinedReport updateData) {
        CombinedReport report = combinedReportRepository.findById(id)
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

        return combinedReportRepository.save(report);
    }

    public void deleteReport(Long id) {
        combinedReportRepository.deleteById(id);
    }
}
