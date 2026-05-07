package com.dailyreport.service;

import com.dailyreport.model.dto.GitCommitDTO;
import com.dailyreport.model.dto.LLMReportResult;
import com.dailyreport.repository.AppSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
                  "completedTasks": "用编号列表（1. 2. 3.）列出今日完成的工作，归纳总结而非逐条翻译commit message",
                  "inProgressTasks": "用编号列表（1. 2. 3.）列出行中或待完成的工作",
                  "notes": "用编号列表（1. 2. 3.）列出备注、风险项、需要协调的事项，没有则留空"
                }
                要求：
                - 用简洁专业的中文
                - 每一条用"编号+句号+空格"开头，例如"1. 修复了登录模块的空指针问题"
                - 不要逐条翻译commit message，要归纳总结
                - 如果提交记录为空，返回空字段即可

                提交记录：
                %s
                """.formatted(commitsText);

        String apiUrl = getSetting("ai_api_url", "https://api.openai.com");
        String apiKey = getSetting("ai_api_key", "");
        String modelName = getSetting("ai_model_name", "gpt-4o-mini");

        String response;
        if (apiUrl.contains("xiaomimimo")) {
            response = callMimoApi(apiUrl, apiKey, modelName, prompt);
        } else {
            ChatClient chatClient = buildChatClient(apiUrl, apiKey, modelName);
            response = chatClient.prompt().user(prompt).call().content();
        }

        return parseResponse(response);
    }

    public LLMReportResult generateCombinedReport(Map<String, List<GitCommitDTO>> allCommits) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Map.Entry<String, List<GitCommitDTO>> entry : allCommits.entrySet()) {
            sb.append("【").append(entry.getKey()).append("】\n");
            for (GitCommitDTO c : entry.getValue()) {
                sb.append(String.format("[%s] %s (%s) - 修改文件: %s\n",
                        c.getCommitTime().format(timeFmt),
                        c.getMessage(),
                        c.getAuthor(),
                        String.join(", ", c.getChangedFiles())));
            }
            sb.append("\n");
        }

        String prompt = """
                你是一个技术日报助手。以下是今天所有代码仓库的git提交记录，请分析后生成一份完整的日报。
                请直接返回JSON格式，不要包含markdown代码块标记。
                {
                  "completedTasks": "用编号列表（1. 2. 3.）列出今日完成的工作，归纳总结而非逐条翻译commit message",
                  "inProgressTasks": "用编号列表（1. 2. 3.）列出行中或待完成的工作",
                  "notes": "用编号列表（1. 2. 3.）列出备注、风险项、需要协调的事项，没有则留空"
                }
                要求：
                - 用简洁专业的中文
                - 每一条用"编号+句号+空格"开头，例如"1. 修复了登录模块的空指针问题"
                - 不要逐条翻译commit message，要归纳总结
                - 跨仓库的相似工作可以合并归纳
                - 如果某个仓库没有提交记录，不需要提及

                各仓库提交记录：
                %s
                """.formatted(sb.toString());

        String apiUrl = getSetting("ai_api_url", "https://api.openai.com");
        String apiKey = getSetting("ai_api_key", "");
        String modelName = getSetting("ai_model_name", "gpt-4o-mini");

        String response;
        if (apiUrl.contains("xiaomimimo")) {
            response = callMimoApi(apiUrl, apiKey, modelName, prompt);
        } else {
            ChatClient chatClient = buildChatClient(apiUrl, apiKey, modelName);
            response = chatClient.prompt().user(prompt).call().content();
        }

        return parseResponse(response);
    }

    public String generateWeeklyReport(Map<String, List<GitCommitDTO>> allCommits, LocalDate weekStart, LocalDate weekEnd) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (Map.Entry<String, List<GitCommitDTO>> entry : allCommits.entrySet()) {
            sb.append("【").append(entry.getKey()).append("】\n");
            for (GitCommitDTO c : entry.getValue()) {
                sb.append(String.format("[%s] %s (%s)\n",
                        c.getCommitTime().format(timeFmt),
                        c.getMessage(),
                        c.getAuthor()));
            }
            sb.append("\n");
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M月d日");
        String prompt = """
                你是一个技术周报助手。以下是一周内所有代码仓库的git提交记录，请分析后生成一份周报。
                周报要求按项目（仓库）分组，每个项目单独列出本周做了什么。
                请直接返回纯文本，不要返回JSON格式。
                用中文回答，格式如下：

                # 周报（%s - %s）

                ## 项目一：xxx
                本周完成：
                1. xxx
                2. xxx
                进行中：
                1. xxx
                备注：xxx

                ## 项目二：xxx
                本周完成：
                1. xxx
                2. xxx
                进行中：
                1. xxx
                备注：xxx

                ...

                要求：
                - 每个项目（仓库）单独一个章节
                - 用编号列表（1. 2. 3.）列出工作内容
                - 归纳总结，不要逐条翻译commit message
                - 用简洁专业的中文
                - 如果某个仓库没有提交记录，不需要提及

                各仓库提交记录：
                %s
                """.formatted(weekStart.format(dateFmt), weekEnd.format(dateFmt), sb.toString());

        String apiUrl = getSetting("ai_api_url", "https://api.openai.com");
        String apiKey = getSetting("ai_api_key", "");
        String modelName = getSetting("ai_model_name", "gpt-4o-mini");

        String response;
        if (apiUrl.contains("xiaomimimo")) {
            response = callMimoApi(apiUrl, apiKey, modelName, prompt);
        } else {
            ChatClient chatClient = buildChatClient(apiUrl, apiKey, modelName);
            response = chatClient.prompt().user(prompt).call().content();
        }

        return response;
    }

    public String generateMonthlyReport(Map<String, List<GitCommitDTO>> allCommits, int year, int month) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (Map.Entry<String, List<GitCommitDTO>> entry : allCommits.entrySet()) {
            sb.append("【").append(entry.getKey()).append("】\n");
            for (GitCommitDTO c : entry.getValue()) {
                sb.append(String.format("[%s] %s (%s)\n",
                        c.getCommitTime().format(timeFmt),
                        c.getMessage(),
                        c.getAuthor()));
            }
            sb.append("\n");
        }

        String prompt = """
                你是一个技术月报助手。以下是一个月内所有代码仓库的git提交记录，请分析后生成一份月报。
                月报要求按项目（仓库）分组，每个项目单独列出本月做了什么。
                请直接返回纯文本，不要返回JSON格式。
                用中文回答，格式如下：

                # 月报（%d年%d月）

                ## 项目一：xxx
                本月完成：
                1. xxx
                2. xxx
                进行中：
                1. xxx
                备注：xxx

                ## 项目二：xxx
                本月完成：
                1. xxx
                2. xxx
                进行中：
                1. xxx
                备注：xxx

                ...

                要求：
                - 每个项目（仓库）单独一个章节
                - 用编号列表（1. 2. 3.）列出工作内容
                - 归纳总结，不要逐条翻译commit message
                - 用简洁专业的中文
                - 如果某个仓库没有提交记录，不需要提及

                各仓库提交记录：
                %s
                """.formatted(year, month, sb.toString());

        String apiUrl = getSetting("ai_api_url", "https://api.openai.com");
        String apiKey = getSetting("ai_api_key", "");
        String modelName = getSetting("ai_model_name", "gpt-4o-mini");

        String response;
        if (apiUrl.contains("xiaomimimo")) {
            response = callMimoApi(apiUrl, apiKey, modelName, prompt);
        } else {
            ChatClient chatClient = buildChatClient(apiUrl, apiKey, modelName);
            response = chatClient.prompt().user(prompt).call().content();
        }

        return response;
    }

    private String callMimoApi(String apiUrl, String apiKey, String modelName, String prompt) {
        String url = apiUrl.replaceAll("/+$", "");
        if (!url.endsWith("/v1/chat/completions")) {
            url += "/v1/chat/completions";
        }

        RestClient restClient = RestClient.builder().build();

        String body = restClient.post()
                .uri(url)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", modelName,
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "temperature", 0.3,
                        "stream", false
                ))
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = new ObjectMapper().readTree(body);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            return body;
        }
    }

    private ChatClient buildChatClient(String apiUrl, String apiKey, String modelName) {
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
