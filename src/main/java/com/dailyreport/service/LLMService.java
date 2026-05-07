package com.dailyreport.service;

import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.model.dto.LLMReportResult;
import com.dailyreport.repository.AppSettingsRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LLMService {

    private final AppSettingsRepository settingsRepository;

    public LLMService(AppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public LLMReportResult generateDailyReport(List<GitCommitDTO> commits) {
        String commitsText = formatCommits(commits);

        String prompt = """
                你是一个技术日报助手。根据以下git提交记录，生成一份简洁的工作日报。
                请直接返回JSON格式，不要包含markdown代码块标记。
                {
                  "completedTasks": "今日完成的工作，归纳总结而非逐条翻译commit message",
                  "inProgressTasks": "进行中或待完成的工作",
                  "notes": "备注、风险项、需要协调的事项，没有则留空"
                }
                要求：用简洁专业的中文，不要逐条翻译commit message，要归纳总结。
                如果提交记录为空，返回空字段即可。

                提交记录：
                %s
                """.formatted(commitsText);

        ChatClient chatClient = buildChatClient();
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseResponse(response);
    }

    private ChatClient buildChatClient() {
        String apiUrl = getSetting("ai_api_url", "https://api.openai.com");
        String apiKey = getSetting("ai_api_key", "");
        String modelName = getSetting("ai_model_name", "gpt-4o-mini");

        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(apiUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
                .build();

        return ChatClient.builder(model).build();
    }

    private String getSetting(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(s -> s.getSettingValue())
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    private String formatCommits(List<GitCommitDTO> commits) {
        if (commits == null || commits.isEmpty()) {
            return "（无提交记录）";
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        return commits.stream()
                .map(c -> String.format("[%s] %s (%s) - 修改文件: %s",
                        c.getCommitTime().format(timeFmt),
                        c.getMessage(),
                        c.getAuthor(),
                        String.join(", ", c.getChangedFiles())))
                .collect(Collectors.joining("\n"));
    }

    private LLMReportResult parseResponse(String response) {
        LLMReportResult result = new LLMReportResult();

        if (response == null || response.isBlank()) {
            return result;
        }

        String json = response.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```json\\s*", "");
            json = json.replaceFirst("```\\s*$", "");
            json = json.trim();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            LLMReportResult parsed = mapper.readValue(json, LLMReportResult.class);
            result.setCompletedTasks(parsed.getCompletedTasks());
            result.setInProgressTasks(parsed.getInProgressTasks());
            result.setNotes(parsed.getNotes());
        } catch (Exception e) {
            result.setCompletedTasks(json);
        }

        return result;
    }
}
