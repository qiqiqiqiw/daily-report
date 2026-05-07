# Git Daily Report

从 Git 提交记录自动生成工作日报、周报和月报的本地工具。通过日历界面查看、编辑、复制报告，支持多仓库管理和 AI 智能总结。

## 功能

- **日报** — 日历视图，点击日期自动生成当天所有仓库的工作汇总
- **周报** — 按周查看，按项目分组展示本周工作内容
- **月报** — 按月查看，按项目分组展示本月工作内容
- **多仓库管理** — 添加/删除多个本地 Git 仓库，统一分析
- **AI 生成** — 调用大模型（OpenAI 兼容接口）归纳总结提交记录
- **编辑 & 复制** — 生成后可手动编辑，一键复制纯文本
- **设置** — 配置 AI API 地址、密钥、模型名称

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4.5 + Java 17 |
| AI | Spring AI 1.0.0（OpenAI 兼容） |
| Git | JGit 7.1.0 |
| 数据库 | H2 文件模式 |
| 前端 | 原生 HTML/CSS/JS |

## 快速开始

### 前置条件

- Java 17+
- Maven 3.6+
- 一个 OpenAI 兼容的 API（OpenAI / DeepSeek / MiMo 等）

### 启动

```bash
# 克隆项目
git clone <repo-url>
cd daily-report

# 编译运行
mvn spring-boot:run
```

访问 http://localhost:8080

### 配置 AI

启动后点击右上角菜单 → 设置，填写：

- **API 地址** — 如 `https://api.openai.com`、`https://api.deepseek.com`
- **API 密钥** — 你的 API Key
- **模型名称** — 如 `gpt-4o-mini`、`deepseek-chat`

支持的 API 提供商：

| 提供商 | API 地址 |
|--------|----------|
| OpenAI | `https://api.openai.com` |
| DeepSeek | `https://api.deepseek.com` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode` |
| MiMo | `https://api.xiaomimimo.com` |
| 本地 Ollama | `http://localhost:11434` |

### 使用

1. 点击菜单 → 添加仓库，输入本地 Git 仓库的绝对路径
2. 回到日历，点击日期 → 生成，AI 自动分析所有仓库的提交记录
3. 顶部导航可切换到周报、月报页面

## API 接口

### 仓库管理

```
GET    /api/repositories          # 仓库列表
POST   /api/repositories          # 添加仓库
DELETE /api/repositories/{id}     # 删除仓库
```

### 日报

```
GET    /api/combined-reports?month=2026-05   # 某月日报列表
GET    /api/combined-reports/{date}          # 某天日报
POST   /api/combined-reports/generate        # 生成日报
PUT    /api/combined-reports/{id}            # 编辑日报
DELETE /api/combined-reports/{id}            # 删除日报
```

### 周报

```
GET    /api/weekly-reports/{date}            # 某周周报
POST   /api/weekly-reports/generate          # 生成周报
PUT    /api/weekly-reports/{id}              # 编辑周报
DELETE /api/weekly-reports/{id}              # 删除周报
```

### 月报

```
GET    /api/monthly-reports/{year}/{month}   # 某月月报
POST   /api/monthly-reports/generate         # 生成月报
PUT    /api/monthly-reports/{id}             # 编辑月报
DELETE /api/monthly-reports/{id}             # 删除月报
```

### 设置

```
GET    /api/settings               # 获取设置
PUT    /api/settings               # 更新设置
```

## 项目结构

```
src/main/java/com/dailyreport/
├── config/           # CORS 配置
├── controller/       # REST 接口
├── model/            # 实体和 DTO
├── repository/       # JPA Repository
└── service/          # 业务逻辑（Git、LLM、日报、周报、月报）

src/main/resources/
├── application.yml
└── static/
    ├── index.html        # 日报主页
    ├── weekly.html       # 周报页
    ├── monthly.html      # 月报页
    ├── css/style.css
    └── js/
        ├── api.js        # 基础请求封装
        ├── calendar.js   # 日历组件
        ├── combined.js   # 日报逻辑
        ├── weekly.js     # 周报逻辑
        └── monthly.js    # 月报逻辑
```

## 许可证

MIT
