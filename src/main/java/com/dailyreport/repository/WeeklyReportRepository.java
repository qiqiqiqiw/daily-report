package com.dailyreport.repository;

import com.dailyreport.model.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findByWeekStartDate(LocalDate weekStartDate);
}
