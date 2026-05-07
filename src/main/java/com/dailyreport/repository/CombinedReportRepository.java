package com.dailyreport.repository;

import com.dailyreport.model.CombinedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CombinedReportRepository extends JpaRepository<CombinedReport, Long> {

    Optional<CombinedReport> findByReportDate(LocalDate reportDate);

    @Query("SELECT r FROM CombinedReport r WHERE r.reportDate >= :startDate AND r.reportDate <= :endDate")
    List<CombinedReport> findByReportDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
