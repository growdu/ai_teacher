# 架构设计文档

## 1. 项目概述

### 1.1 项目名称
**AI Teacher Studio** - 智能教学资源生成平台

### 1.2 项目定位
面向教育行业，将教学知识点快速转换为教学视频、教学PPT、测验试题等多元教学资源的智能生成平台。

### 1.3 核心场景
```
知识点输入 → 智能生成 → 教学视频 + 教学PPT + 随堂测验
```

### 1.4 设计原则
| 原则 | 说明 |
|------|------|
| 一键生成 | 输入知识点，自动完成从大纲到各类资源的全流程 |
| 模块化解耦 | 知识点、课程、PPT、视频、测验各自独立，接口清晰 |
| 厂商无关 | 通过抽象层接入各类AI厂商 |
| 异步处理 | 长任务（视频生成）异步执行，不阻塞主流程 |
| 理科优先准确 | 数学/物理/化学内容必须保证公式/单位/推导步骤正确 |
| 互动性内置 | 教学资源必须内置师生互动设计 |

---

## 2. 顶层模块架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                          用户层（Web / API）                          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
         ┌───────────────────┼────────────────────┐
         │                   │                    │
         ▼                   ▼                    ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  知识点管理模块   │ │   课程管理模块   │ │   测验管理模块   │
│ KnowledgePoint   │ │     Course       │ │      Quiz       │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                  │                    │
         └──────────────────┼────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        TeachingMaterial（教学材料）                    │
│            统一的材料存储：一个 Course 可生成多个 TeachingMaterial        │
│                     type=ppt / type=video / type=quiz                 │
└─────────────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ PPT 生成模块      │ │ 视频生成模块      │ │ 测验生成模块      │
│ PptGeneration   │ │ VideoGeneration │ │ QuizGeneration  │
│                 │ │                 │ │                 │
│ 输入：Course     │ │ 输入：Course     │ │ 输入：Course    │
│ 输出：PPTX文件    │ │ 输出：MP4视频    │ │ 输出：JSON题目  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │   AI 能力层（统一）    │
                    │  LLM / TTS / Image  │
                    │  MiniMax / OpenAI   │
                    │  GLM / DeepSeek     │
                    └─────────────────────┘
```

---

## 3. 模块依赖关系

```
KnowledgePoint（知识点）
    │
    │  1:N — 一个知识点可生成 N 个课程
    ▼
Course（课程）
    │
    │  ┌──────────────────────────────────┐
    │  │  1:1 包含 CourseOutline JSON    │
    │  │  • chapters[] (章节结构)        │
    │  │  • teachingNotes[] (教学备注)    │
    │  │  • keyPoints[] (重点标记)        │
    │  └──────────────────────────────────┘
    │
    ├──────────────────┬──────────────────┬──────────────────┐
    │                  │                  │                  │
    ▼                  ▼                  ▼                  ▼
PptGeneration    VideoGeneration    QuizGeneration    （未来扩展）
    │                  │                  │
    ▼                  ▼                  ▼
TeachingMaterial  TeachingMaterial  Quiz JSON
(type=ppt)       (type=video)     (通过API返回)
```

**关键约束**：
- `Course.outline` 是 PPT 模块的唯一数据来源（JSON格式）
- `Course.script` 是视频模块的主要数据来源
- 所有生成模块均不直接依赖 `KnowledgePoint`，只通过 `Course` 中转
- `TeachingMaterial` 是所有生成物的统一存储表

---

## 4. 核心模块职责

| 模块 | 职责 | 核心输入 | 核心输出 |
|------|------|---------|---------|
| 知识点管理 | 知识点的 CRUD、标签、搜索 | 学科/年级/内容 | KnowledgePoint |
| 课程管理 | 从知识点生成课程结构（大纲+讲稿） | KnowledgePoint | Course（带outline+script） |
| PPT生成 | 根据课程大纲生成精美教学PPT | Course.outline | TeachingMaterial(type=ppt) |
| 视频生成 | 根据讲稿生成数字人/配音视频 | Course.script | TeachingMaterial(type=video) |
| 测验生成 | 根据课程内容生成随堂测验题 | Course | JSON测验题目 |

---

## 5. 数据模型

### 5.1 知识点表（knowledge_point）

```sql
CREATE TABLE knowledge_point (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL,
    workspace_id    BIGINT NOT NULL,
    subject         VARCHAR(64)      COMMENT '学科：数学/物理/化学/语文/历史...',
    grade           VARCHAR(32)      COMMENT '年级：初一/高一/大一...',
    content         TEXT             COMMENT '知识点正文',
    tags            VARCHAR(255)     COMMENT '标签，逗号分隔',
    difficulty      VARCHAR(16)      COMMENT '难度：easy/medium/hard',
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         BOOLEAN DEFAULT FALSE,
    INDEX idx_tenant_subject (tenant_id, subject),
    INDEX idx_workspace (workspace_id)
);
```

### 5.2 课程表（course）

```sql
CREATE TABLE course (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           BIGINT NOT NULL,
    workspace_id        BIGINT NOT NULL,
    knowledge_point_id  BIGINT NOT NULL     COMMENT '关联知识点',
    title               VARCHAR(255),
    outline             JSON                COMMENT '课程大纲JSON（chapters+教学设计）',
    script              TEXT                COMMENT '讲稿脚本',
    status              VARCHAR(32)          COMMENT 'draft/generated/published',
    subject_type        VARCHAR(16)          COMMENT 'science/humanities',
    target_audience     VARCHAR(32)          COMMENT '小学生/初中生/高中生/大学生',
    total_duration      INT                  COMMENT '总时长（分钟）',
    creator_id          BIGINT,
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted             BOOLEAN DEFAULT FALSE,
    INDEX idx_knowledge_point (knowledge_point_id),
    INDEX idx_tenant (tenant_id)
);
```

**outline JSON 结构**：
```json
{
  "title": "课程标题",
  "description": "课程简介",
  "subjectType": "science",
  "targetAudience": "初中生",
  "totalDuration": 40,
  "chapters": [
    {
      "title": "第一章：xxx",
      "duration": 10,
      "keyPoints": ["重点1", "重点2"],
      "teachingNotes": "教学备注",
      "slides": []  // AIEnrichmentService 充实后的幻灯片内容
    }
  ],
  "narrativeArc": {
    "opening": "开场问题",
    "exploration": "探索阶段",
    "discovery": "发现时刻",
    "application": "应用阶段"
  },
  "learningObjectives": ["目标1", "目标2"]
}
```

### 5.3 教学材料表（teaching_material）

```sql
CREATE TABLE teaching_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT NOT NULL,
    workspace_id    BIGINT NOT NULL,
    course_id       BIGINT NOT NULL         COMMENT '关联课程',
    material_type   VARCHAR(32)            COMMENT 'ppt/video/quiz',
    title           VARCHAR(255),
    file_url        VARCHAR(512)            COMMENT '文件存储URL（ppt/video）',
    thumbnail_url   VARCHAR(512),
    duration        INT                     COMMENT '视频时长（秒）',
    file_size       BIGINT,
    status          VARCHAR(32)             COMMENT 'pending/generating/ready/failed',
    metadata        JSON                    COMMENT '扩展字段（如视频的taskId）',
    creator_id      BIGINT,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted         BOOLEAN DEFAULT FALSE,
    INDEX idx_course (course_id),
    INDEX idx_material_type (material_type)
);
```

### 5.4 测验题目表（quiz_question，可选扩展）

```sql
CREATE TABLE quiz_question (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id       BIGINT NOT NULL,
    question_type   VARCHAR(32)             COMMENT 'choice/blank/essay',
    content         TEXT,
    options         JSON,
    answer          TEXT,
    explanation     TEXT,
    difficulty      VARCHAR(16),
    created_at      DATETIME,
    deleted         BOOLEAN DEFAULT FALSE,
    INDEX idx_course (course_id)
);
```

---

## 6. 多租户设计

### 6.1 租户模型

```
Tenant（机构/学校）
    └── Workspace（工作空间/班级）
            ├── 知识点库（KnowledgePoint）
            ├── 课程（Course）
            ├── 生成的PPT（TeachingMaterial.type=ppt）
            ├── 生成的视频（TeachingMaterial.type=video）
            └── 测验记录（QuizQuestion）
```

### 6.2 角色

| 角色 | 权限 |
|------|------|
| 管理员 | 机构管理、额度配置、全部工作空间 |
| 教师 | 创建知识点/课程/生成资源、查看/下载自己的资源 |
| 制作人员 | 视频制作、后期处理 |
| 审核员 | 审核生成的资源 |

---

## 7. 技术架构要点

### 7.1 核心设计原则
- **一键生成**：输入知识点，自动完成从大纲到视频的全流程
- **模块化解耦**：知识理解、内容生成、媒体合成独立模块
- **厂商无关**：通过抽象层接入各类AI厂商
- **异步处理**：视频生成等长任务异步执行，不阻塞主流程
- **理科准确性优先**：数学/物理/化学内容必须保证公式/单位/推导步骤正确
- **互动性内置**：教学资源必须内置师生互动节点

### 7.2 可扩展方向
- 题库管理（QuizQuestion 持久化）
- 微课生成
- 教案生成
- 试卷生成

---

## 8. 模块间接口约定

### 8.1 课程→PPT
```
输入：Course.outline（JSON）
输出：TeachingMaterial(type=ppt, fileUrl=PPTX文件URL)
```

### 8.2 课程→视频
```
输入：Course.script + 可选 Course.outline
输出：TeachingMaterial(type=video, fileUrl=MP4文件URL)
```

### 8.3 课程→测验
```
输入：Course.title + Course.script
输出：JSON { courseId, questions[] }
```

---

## 9. 各模块详细设计索引

| 模块 | 文档 |
|------|------|
| 知识点管理 | `docs/knowledge-management-design.md` |
| 课程管理 | `docs/course-design.md` |
| PPT生成 | `docs/ppt-generation-design.md` |
| 视频生成 | `docs/video-generation-design.md` |
| 测验模块 | `docs/quiz-design.md` |
