package com.dailyreport.repository;

import com.dailyreport.model.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {
}
