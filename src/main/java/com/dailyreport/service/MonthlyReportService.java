package com.dailyreport.service;

import com.dailyreport.model.GitRepository;
import com.dailyreport.model.MonthlyReport;
import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.repository.GitRepositoryRepository;
import com.dailyreport.repository.MonthlyReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitService gitService;
    private final LLMService llmService;

    public MonthlyReportService(MonthlyReportRepository monthlyReportRepository,
                                 GitRepositoryRepository gitRepositoryRepository,
                                 GitService gitService,
                                 LLMService llmService) {
        this.monthlyReportRepository = monthlyReportRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.gitService = gitService;
        this.llmService = llmService;
    }

    public MonthlyReport generateAndSave(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        String yearMonth = String.format("%d-%02d", year, month);

        List<GitRepository> repos = gitRepositoryRepository.findAll();
        if (repos.isEmpty()) {
            throw new RuntimeException("没有已添加的仓库");
        }

        Map<String, List<GitCommitDTO>> allCommits = new LinkedHashMap<>();
        for (GitRepository repo : repos) {
            List<GitCommitDTO> monthCommits = new ArrayList<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                try {
                    List<GitCommitDTO> dayCommits = gitService.getCommits(repo.getLocalPath(), date);
                    monthCommits.addAll(dayCommits);
                } catch (Exception e) {
                    // 仓库读取失败跳过
                }
            }
            if (!monthCommits.isEmpty()) {
                allCommits.put(repo.getName(), monthCommits);
            }
        }

        if (allCommits.isEmpty()) {
            throw new RuntimeException("本月所有仓库均无提交记录");
        }

        String content = llmService.generateMonthlyReport(allCommits, year, month);

        Optional<MonthlyReport> existing = monthlyReportRepository.findByYearMonth(yearMonth);
        MonthlyReport report = existing.orElse(new MonthlyReport());
        report.setYearMonth(yearMonth);
        report.setContent(content);
        report.setIsEdited(false);

        return monthlyReportRepository.save(report);
    }

    public MonthlyReport getByYearMonth(int year, int month) {
        String yearMonth = String.format("%d-%02d", year, month);
        return monthlyReportRepository.findByYearMonth(yearMonth).orElse(null);
    }

    public MonthlyReport updateReport(Long id, MonthlyReport updateData) {
        MonthlyReport report = monthlyReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("月报不存在: " + id));

        if (updateData.getContent() != null) {
            report.setContent(updateData.getContent());
        }
        report.setIsEdited(true);

        return monthlyReportRepository.save(report);
    }

    public void deleteReport(Long id) {
        monthlyReportRepository.deleteById(id);
    }
}
