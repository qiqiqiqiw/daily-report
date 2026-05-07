package com.dailyreport.controller;

import com.dailyreport.model.AppSettings;
import com.dailyreport.repository.AppSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private static final String KEY_AUTO_ENABLED = "auto_generate_enabled";
    private static final String KEY_CRON = "auto_generate_cron";
    private static final String KEY_DEFAULT_REPO = "default_repository_id";
    private static final String KEY_AI_API_URL = "ai_api_url";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_MODEL = "ai_model_name";

    private final AppSettingsRepository settingsRepository;

    public SettingsController(AppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("autoGenerateEnabled", getSettingValue(KEY_AUTO_ENABLED, "false"));
        settings.put("autoGenerateCron", getSettingValue(KEY_CRON, "0 0 18 * * ?"));
        settings.put("defaultRepositoryId", getSettingValue(KEY_DEFAULT_REPO, null));
        settings.put("aiApiUrl", getSettingValue(KEY_AI_API_URL, ""));
        settings.put("aiApiKey", getSettingValue(KEY_AI_API_KEY, ""));
        settings.put("aiModelName", getSettingValue(KEY_AI_MODEL, ""));
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        if (body.containsKey("autoGenerateEnabled")) {
            saveSetting(KEY_AUTO_ENABLED, String.valueOf(body.get("autoGenerateEnabled")));
        }
        if (body.containsKey("autoGenerateCron")) {
            saveSetting(KEY_CRON, String.valueOf(body.get("autoGenerateCron")));
        }
        if (body.containsKey("defaultRepositoryId")) {
            Object val = body.get("defaultRepositoryId");
            saveSetting(KEY_DEFAULT_REPO, val == null ? null : String.valueOf(val));
        }
        if (body.containsKey("aiApiUrl")) {
            saveSetting(KEY_AI_API_URL, String.valueOf(body.get("aiApiUrl")));
        }
        if (body.containsKey("aiApiKey")) {
            saveSetting(KEY_AI_API_KEY, String.valueOf(body.get("aiApiKey")));
        }
        if (body.containsKey("aiModelName")) {
            saveSetting(KEY_AI_MODEL, String.valueOf(body.get("aiModelName")));
        }
        return getSettings();
    }

    private String getSettingValue(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(AppSettings::getSettingValue)
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value) {
        AppSettings setting = settingsRepository.findBySettingKey(key)
                .orElse(new AppSettings(key, value));
        setting.setSettingValue(value);
        settingsRepository.save(setting);
    }
}
