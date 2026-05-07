# Git Daily Report - AI开发需求文档

## 1. 项目概述

一个本地运行的工具，自动从Git提交记录生成工作日报和待办事项。用户通过简洁的日历界面查看、编辑每天的日报。核心价值：**把开发者已经做过的事情自动记录下来，整理成别人能看懂的文字**。

## 2. 技术栈

| 层次 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.4.5 + Java 17 |
| AI集成 | Spring AI 1.0.0 (OpenAI兼容接口) |
| Git读取 | JGit 7.1.0 |
| 数据库 | H2 (文件模式，本地存储) |
| ORM | Spring Data JPA |
| 前端 | 原生HTML/CSS/JS + FullCalendar.js (CDN引入) |
| 构建工具 | Maven |

## 3. 项目目录结构

```
daily-report/
├── pom.xml
├── REQUIREMENTS.md
├── src/main/
│   ├── java/com/dailyreport/
│   │   ├── DailyReportApplication.java        # 启动类 (已完成)
│   │   ├── config/
│   │   │   └── WebConfig.java                  # CORS配置、静态资源映射
│   │   ├── model/
│   │   │   ├── GitRepository.java              # Git仓库实体
│   │   │   ├── DailyReport.java                # 日报实体
│   │   │   ├── AppSettings.java                # 应用设置实体
│   │   │   └── dto/
│   │   │       ├── GitCommitDTO.java           # Git提交数据传输对象
│   │   │       ├── ReportGenerateRequest.java  # 生成日报请求
│   │   │       └── LLMReportResult.java        # LLM返回的结构化日报
│   │   ├── repository/
│   │   │   ├── GitRepositoryRepository.java
│   │   │   ├── DailyReportRepository.java
│   │   │   └── AppSettingsRepository.java
│   │   ├── service/
│   │   │   ├── GitService.java                 # 读取git日志
│   │   │   ├── LLMService.java                 # 调用AI生成日报
│   │   │   └── ReportService.java              # 日报CRUD + 定时任务
│   │   └── controller/
│   │       ├── RepositoryController.java       # 仓库管理接口
│   │       ├── ReportController.java           # 日报接口
│   │       └── SettingsController.java         # 设置接口
│   └── resources/
│       ├── application.yml                     # 应用配置 (已完成)
│       └── static/
│           ├── index.html                      # 主页面
│           ├── css/style.css                   # 样式
│           └── js/
│               ├── app.js                      # 主逻辑
│               ├── calendar.js                 # 日历组件封装
│               └── api.js                      # API调用封装
```

## 4. 数据库设计

### 4.1 git_repositories 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 仓库名称 |
| local_path | String | 本地仓库路径 |
| created_at | Timestamp | 创建时间 |

### 4.2 daily_reports 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| report_date | LocalDate | 日报日期 |
| repository_id | Long (FK) | 关联仓库 |
| raw_commits | Text | 原始commit JSON |
| completed_tasks | Text | 今日完成的工作 (LLM生成) |
| in_progress_tasks | Text | 进行中的工作 (LLM生成) |
| notes | Text | 备注/风险项 (LLM生成) |
| is_edited | Boolean | 用户是否手动编辑过 |
| created_at | Timestamp | 创建时间 |
| updated_at | Timestamp | 更新时间 |

### 4.3 app_settings 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| setting_key | String (unique) | 设置键 |
| setting_value | String | 设置值 |

存储的key包括：
- `auto_generate_enabled` : 是否开启自动生成 (true/false)
- `auto_generate_cron` : cron表达式，默认 `0 0 18 * * ?` (每天18:00)
- `default_repository_id` : 默认使用的仓库ID

## 5. 后端API设计

### 5.1 仓库管理

```
POST   /api/repositories
  请求体: { "name": "my-project", "localPath": "D:/code/my-project" }
  响应: 201, 返回仓库对象

GET    /api/repositories
  响应: 所有仓库列表

DELETE /api/repositories/{id}
  响应: 204
```

### 5.2 日报

```
GET    /api/reports?month=2026-05&repositoryId=1
  说明: 获取某月所有日报，用于日历展示
  响应: [{ "id":1, "reportDate":"2026-05-06", "completedTasks":"...", "hasReport":true }, ...]
  注意: 日历只需要日期和标记，不需要完整内容

GET    /api/reports/{date}?repositoryId=1
  说明: 获取某天的完整日报
  响应: 完整日报对象，包含所有字段

POST   /api/reports/generate
  请求体: { "date": "2026-05-06", "repositoryId": 1 }
  说明: 手动触发生成某天的日报
  流程: 调用GitService获取commits -> 调用LLMService生成日报 -> 存入数据库
  响应: 生成的日报对象

PUT    /api/reports/{id}
  请求体: { "completedTasks": "修改后的内容", "inProgressTasks": "...", "notes": "..." }
  说明: 用户编辑日报，编辑后is_edited设为true
  响应: 更新后的日报对象

DELETE /api/reports/{id}
  响应: 204
```

### 5.3 设置

```
GET    /api/settings
  响应: { "autoGenerateEnabled": true, "autoGenerateCron": "0 0 18 * * ?", "defaultRepositoryId": 1 }

PUT    /api/settings
  请求体: 同上
  响应: 更新后的设置
```

## 6. 核心Service实现要点

### 6.1 GitService

```
功能: 给定仓库路径和日期范围，返回该时间段内的所有git commits

方法: List<GitCommitDTO> getCommits(String repoPath, LocalDate date)

实现要点:
- 使用JGit打开本地仓库
- 按日期过滤commit (当天00:00到23:59)
- 提取信息: commit message, author, time, 变更的文件列表
- 按时间正序排列
- 如果仓库路径不存在或不是git仓库，抛出业务异常
```

### 6.2 LLMService

```
功能: 接收commits列表，调用LLM生成结构化日报

方法: LLMReportResult generateDailyReport(List<GitCommitDTO> commits)

实现要点:
- 使用Spring AI的ChatClient
- 把commits组装成可读文本
- 通过prompt要求LLM返回JSON格式
- 使用entity()方法自动映射为Java对象
- 返回的LLMReportResult包含: completedTasks, inProgressTasks, notes

Prompt模板:
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
  {commits文本}
```

### 6.3 ReportService

```
功能: 日报的业务逻辑 + 定时任务

方法:
- DailyReport generateAndSave(LocalDate date, Long repositoryId)
  -> 调用GitService获取commits -> 调用LLMService生成 -> 存入数据库
  -> 如果当天已有日报且is_edited=true，不覆盖，需要用户确认

- List<DailyReport> getMonthlyReports(int year, int month, Long repositoryId)

- DailyReport getByDate(LocalDate date, Long repositoryId)

- DailyReport updateReport(Long id, DailyReport updateData)

定时任务:
  @Scheduled(cron = "${daily-report.cron}")
  读取auto_generate_enabled设置，如果开启则对默认仓库自动生成当天日报
```

## 7. 前端设计

### 7.1 页面布局

```
┌─────────────────────────────────────────────┐
│  Git Daily Report          [+ 添加仓库] [设置] │
├─────────────────────────────────────────────┤
│                                             │
│              ┌──────────────┐               │
│              │  5月 2026     │    <  2026年5月  >  │
│  一  二  三  四  五  六  日                    │
│              ...   1  2  3  4                │
│   5  6  7  8  9  10 11                       │
│  12 13 14 15 16 17 18                        │
│  ...                                        │
│                                             │
│  (有日报的日期格子右上角显示小圆点标记)         │
│                                             │
└─────────────────────────────────────────────┘
```

### 7.2 日报弹窗 (Modal)

点击日历上的某一天，弹出模态窗口：

```
┌─────────────────────────────────────────────┐
│  2026年5月6日 周三 的工作日报      [编辑] [生成] │
├─────────────────────────────────────────────┤
│                                             │
│  📋 今日完成                                 │
│  ─────────                                  │
│  修复了用户登录模块的空指针问题                │
│  完成了订单列表接口的性能优化                  │
│                                             │
│  🔄 进行中                                   │
│  ─────────                                  │
│  用户权限模块重构，预计明天完成                │
│                                             │
│  📝 备注                                     │
│  ─────────                                  │
│  订单接口优化需要关注线上监控                  │
│                                             │
│                        [复制] [关闭]          │
└─────────────────────────────────────────────┘
```

- 如果当天没有日报，显示"暂无日报，点击[生成]按钮创建"
- [编辑] 按钮: 切换为文本域编辑模式，保存时调用PUT接口
- [生成] 按钮: 调用生成接口，加载中显示spinner
- [复制] 按钮: 把日报内容复制到剪贴板，格式化的纯文本

### 7.3 设置弹窗

```
┌─────────────────────────────────────────────┐
│  设置                                        │
├─────────────────────────────────────────────┤
│                                             │
│  默认仓库:  [下拉选择]                        │
│                                             │
│  自动生成日报:  [开关]                         │
│                                             │
│  生成时间:  [18:00]                           │
│                                             │
│                              [保存] [取消]    │
└─────────────────────────────────────────────┘
```

### 7.4 添加仓库弹窗

```
┌─────────────────────────────────────────────┐
│  添加Git仓库                                 │
├─────────────────────────────────────────────┤
│                                             │
│  仓库名称:  [___________]                    │
│                                             │
│  本地路径:  [___________]  [浏览]             │
│                                             │
│                              [添加] [取消]    │
└─────────────────────────────────────────────┘
```

### 7.5 技术实现要点

- 使用FullCalendar.js的日历视图 (dayGridMonth)
- 用eventSources加载已有的日报数据作为日历标记
- 点击日期(dateClick事件)打开Modal
- Modal内容通过AJAX调用后端接口动态加载
- 样式简洁，参考现代SaaS风格，浅色主题
- 响应式布局，适配不同屏幕尺寸

## 8. application.yml 配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/dailyreport
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key-here}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.3

daily-report:
  cron: "0 0 18 * * ?"
  auto-generate-enabled: false
```

支持通过环境变量切换LLM提供商：
- OpenAI: OPENAI_BASE_URL=https://api.openai.com
- DeepSeek: OPENAI_BASE_URL=https://api.deepseek.com
- 通义千问: OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
- 本地Ollama: OPENAI_BASE_URL=http://localhost:11434

## 9. 已完成的文件

- `pom.xml` - Maven项目配置，所有依赖已添加
- `src/main/java/com/dailyreport/DailyReportApplication.java` - Spring Boot启动类，已启用@EnableScheduling

## 10. 待实现的文件清单

按开发顺序排列：

### 第一阶段 - 后端基础
1. `config/WebConfig.java` - CORS配置
2. `model/GitRepository.java` - JPA实体
3. `model/DailyReport.java` - JPA实体
4. `model/AppSettings.java` - JPA实体
5. `model/dto/GitCommitDTO.java` - DTO
6. `model/dto/ReportGenerateRequest.java` - DTO
7. `model/dto/LLMReportResult.java` - DTO
8. `repository/GitRepositoryRepository.java` - JPA Repository
9. `repository/DailyReportRepository.java` - JPA Repository
10. `repository/AppSettingsRepository.java` - JPA Repository

### 第二阶段 - 核心Service
11. `service/GitService.java` - 读取git日志
12. `service/LLMService.java` - 调用AI生成日报
13. `service/ReportService.java` - 日报业务逻辑 + 定时任务

### 第三阶段 - REST接口
14. `controller/RepositoryController.java`
15. `controller/ReportController.java`
16. `controller/SettingsController.java`

### 第四阶段 - 前端
17. `resources/static/index.html`
18. `resources/static/css/style.css`
19. `resources/static/js/api.js`
20. `resources/static/js/calendar.js`
21. `resources/static/js/app.js`
