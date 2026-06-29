# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-06-29

### Added

- **PPT 生成（多样化幻灯片类型）**
  - 新增 8 种幻灯片类型：problem、concept、derivation、exercise、infographic、comic、vote/poll、exit-ticket
  - 理科"问题驱动四步法"：问题引入→概念讲解→例题演练→总结提升
  - 文科趣味性叙事弧：开场→探索→发现→应用
  - AIEnrichmentService：按章节逐章生成多样化幻灯片内容
  - MockLLMProvider 支持全部新幻灯片类型的 JSON 输出

- **AI 模型选择**
  - 前端 MaterialPage 支持 AI 模型下拉选择
  - 后端按模型名称路由到对应 Provider
  - MODEL_NAME_ALIASES 支持多组别名映射

- **图片生成**
  - Pollinations.ai 免费图片生成（无需 API key）
  - 60s 超时 + 429 重试机制
  - fallback 到形状渲染

- **LandingPage + 路由**
  - `/` → 公开 LandingPage
  - `/app` → 受保护后台
  - 全部内链统一加 `/app` 前缀

- **MockLLMProvider 教学幻灯片结构化输出**
  - 完整 JSON Schema：chapterTitle、narrativeArc、learningObjectives、slides[]
  - 每章节生成 problem + concept + derivation + exercise + infographic + comic + quiz + vote/exit-ticket

### Fixed

- **MockLLMProvider 路由**：修复 `modelName="mock"` 无法匹配到 `MockLLMProvider` 的问题
  - AIProviderRegistry 新增 `findLLMProvider(name)` 支持 case-insensitive key 查找
  - AIService MODEL_NAME_ALIASES 修复 Map.of() 10对上限问题，改为 HashMap static initializer
  - CircuitBreaker fallback 方法签名修复：`chat(request, modelName)` 移除无效 fallback 注解，新增 `chatResponseFallback(request, modelName, t)`
  - 查找顺序调整：先 key 直接匹配，再用别名

- **Nginx 超时**：proxy_read_timeout 60s → 300s，避免长流程 PPT 生成超时

- **normalizeColor**：8位 ARGB → 6位 HEX 颜色修复

- **AI Provider 双注册点**：AIConfigService.initProviders()(@PostConstruct) 早于 AIProviderConfig.configureAIProviders()(@Bean)，mockEnabled 判断被绕过。已在两处都检查 mockEnabled。

- **PPT 预览三连环 bug**
  1. fetch 无 Auth → 403（手动添加 Authorization 头）
  2. useEffect 直接操作 DOM iframe.src 被 React 覆盖（改用 React state）
  3. 拦截器对 blob 返回 body.data 再用 r.data 得 undefined（直接使用 blob）

- **MiniMax 视频生成路径**：/material/video/generate；状态轮询：/material/task/{taskId}

### Changed

- 后端 Dockerfile：多阶段构建，builder 用 eclipse-temurin:21-jdk，运行时用 21-jre

### Infrastructure

- Docker Compose 项目名：ai_teacher_ai-teacher（下划线连接）
- Nginx 配置：deploy/nginx/nginx.conf
- 后端脚本：backend/scripts/generate-ppt.js
