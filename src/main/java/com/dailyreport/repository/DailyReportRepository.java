package com.dailyreport.repository;

import com.dailyreport.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    @Query("SELECT r FROM DailyReport r WHERE r.repositoryId = :repositoryId " +
           "AND r.reportDate >= :startDate AND r.reportDate <= :endDate")
    List<DailyReport> findByRepositoryIdAndDateRange(
            @Param("repositoryId") Long repositoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    Optional<DailyReport> findByReportDateAndRepositoryId(LocalDate reportDate, Long repositoryId);
}
