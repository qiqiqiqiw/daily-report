package com.dailyreport.repository;

import com.dailyreport.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {

    Optional<AppSettings> findBySettingKey(String settingKey);
}
