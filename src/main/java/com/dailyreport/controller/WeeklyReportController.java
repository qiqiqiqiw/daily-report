package com.dailyreport.controller;

import com.dailyreport.model.WeeklyReport;
import com.dailyreport.service.WeeklyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@RestController
@RequestMapping("/api/weekly-reports")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/{date}")
    public ResponseEntity<WeeklyReport> getByDate(@PathVariable String date) {
        LocalDate anyDate = LocalDate.parse(date);
        LocalDate weekStart = anyDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeeklyReport report = weeklyReportService.getByWeekStart(weekStart);
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            WeeklyReport report = weeklyReportService.generateAndSave(date);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklyReport> update(@PathVariable Long id, @RequestBody WeeklyReport updateData) {
        WeeklyReport report = weeklyReportService.updateReport(id, updateData);
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklyReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
