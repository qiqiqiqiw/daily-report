package com.dailyreport.controller;

import com.dailyreport.model.DailyReport;
import com.dailyreport.model.dto.ReportGenerateRequest;
import com.dailyreport.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReports(
            @RequestParam String month,
            @RequestParam(required = false, defaultValue = "0") Long repositoryId) {
        // month 格式: "2026-05"
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);

        List<DailyReport> reports = reportService.getMonthlyReports(year, mon, repositoryId);

        // 日历只返回轻量数据
        List<Map<String, Object>> result = reports.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "reportDate", r.getReportDate().toString(),
                        "completedTasks", r.getCompletedTasks() != null ? r.getCompletedTasks() : "",
                        "hasReport", true))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{date}")
    public ResponseEntity<DailyReport> getByDate(
            @PathVariable String date,
            @RequestParam(required = false, defaultValue = "0") Long repositoryId) {
        LocalDate reportDate = LocalDate.parse(date);
        DailyReport report = reportService.getByDate(reportDate, repositoryId);
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping
    public ResponseEntity<DailyReport> create(@RequestBody Map<String, Object> body) {
        LocalDate date = LocalDate.parse((String) body.get("date"));
        Long repositoryId = body.get("repositoryId") != null
                ? Long.parseLong(String.valueOf(body.get("repositoryId"))) : 0L;
        DailyReport data = new DailyReport();
        data.setCompletedTasks((String) body.get("completedTasks"));
        data.setInProgressTasks((String) body.get("inProgressTasks"));
        data.setNotes((String) body.get("notes"));
        DailyReport report = reportService.createReport(date, repositoryId, data);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody ReportGenerateRequest request) {
        try {
            DailyReport report = reportService.generateAndSave(request.getDate(), request.getRepositoryId());
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailyReport> update(@PathVariable Long id, @RequestBody DailyReport updateData) {
        DailyReport report = reportService.updateReport(id, updateData);
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
