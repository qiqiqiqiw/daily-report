package com.dailyreport.controller;

import com.dailyreport.model.MonthlyReport;
import com.dailyreport.service.MonthlyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monthly-reports")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    public MonthlyReportController(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<MonthlyReport> getByYearMonth(@PathVariable int year, @PathVariable int month) {
        MonthlyReport report = monthlyReportService.getByYearMonth(year, month);
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        try {
            int year = (int) body.get("year");
            int month = (int) body.get("month");
            MonthlyReport report = monthlyReportService.generateAndSave(year, month);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonthlyReport> update(@PathVariable Long id, @RequestBody MonthlyReport updateData) {
        MonthlyReport report = monthlyReportService.updateReport(id, updateData);
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        monthlyReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
