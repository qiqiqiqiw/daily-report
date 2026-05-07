package com.dailyreport.controller;

import com.dailyreport.model.CombinedReport;
import com.dailyreport.service.CombinedReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/combined-reports")
public class CombinedReportController {

    private final CombinedReportService combinedReportService;

    public CombinedReportController(CombinedReportService combinedReportService) {
        this.combinedReportService = combinedReportService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReports(@RequestParam String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);

        List<CombinedReport> reports = combinedReportService.getMonthlyReports(year, mon);

        List<Map<String, Object>> result = reports.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "reportDate", r.getReportDate().toString(),
                        "hasReport", true))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{date}")
    public ResponseEntity<CombinedReport> getByDate(@PathVariable String date) {
        LocalDate reportDate = LocalDate.parse(date);
        CombinedReport report = combinedReportService.getByDate(reportDate);
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            CombinedReport report = combinedReportService.generateAndSave(date);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CombinedReport> update(@PathVariable Long id, @RequestBody CombinedReport updateData) {
        CombinedReport report = combinedReportService.updateReport(id, updateData);
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        combinedReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
