# PPT 生成子模块详细设计

## 1. 现状与问题

```
MaterialController (/api/material/ppt/generate)
    │
    ▼
PptGenerationService (Java)
    ├── parseOutline()        ← 只解析骨架数据
    ├── buildPptConfig()     ← 仅提取 title/chapters/keyPoints
    ├── generatePptFile()    ← 调用 Node.js
    └── uploadPptFile()       → MinIO
            │
            ▼
    generate-ppt.js (PptxGenJS)
            │
            ▼
    幻灯片类型：title / chapter / content / summary / end
```

**问题**：

| # | 问题 | 影响 |
|---|------|------|
| 1 | `buildPptConfig()` 只提取课程骨架（标题 + 章节名 + keyPoints） | PPT 正文内容匮乏 |
| 2 | Content Slide 只有要点列表，形式单一 | 教学效果差 |
| 3 | 没有图片、测验、案例分析、名人名言等幻灯片类型 | 缺乏教学设计 |
| 4 | AI 仅用于生成课程大纲，PPT 内容未经 AI 处理 | 内容深度不足 |
| 5 | 所有章节共用同一套幻灯片结构 | 无法体现章节差异 |

---

## 2. 目标

PPT 内容经 AI 二次处理，每张幻灯片包含足够的教学文字、视觉层次和教学设计元素（案例、测验、讨论等）。

---

## 3. 目标架构

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MaterialController                                │
│                   POST /api/material/ppt/generate                       │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│AIEnrichmentService│  │ PptBuildService │  │FileStorageService│
│   (新增)          │  │   (重构)        │  │   (已有)         │
│                   │  │                 │  │                 │
│ 为每个 Chapter     │  │ 将 ChapterContent│  │ 上传 PPT → MinIO │
│ 生成 SlideContent │  │ 组装成 PptConfig │  │                 │
│ (正文/案例/测验…)  │  │                 │  └─────────────────┘
└────────┬─────────┘  └────────┬─────────┘
         │                     │
         │  List<ChapterContent>│
         │◄────────────────────┘
         │                            │
         ▼                            ▼
┌────────────────────────────────────────────────────┐
│              generate-ppt.js (Node.js)              │
│                 (PptxGenJS)                        │
│                                                    │
│  新增幻灯片类型：text | diagram | case | quote |   │
│                quiz | activity | reflection        │
└────────────────────────────────────────────────────┘
```

---

## 4. 核心数据模型

### 4.1 SlideContent（AI 充实后的幻灯片内容）

```java
@Data
@Builder
public class SlideContent {
    private String slideType;    // text | diagram | case | quote | quiz | activity | reflection
    private String title;         // 幻灯片标题
    private String mainBody;      // 主要阐述文字（100-300字）
    private List<String> bullets; // 要点列表
    private List<String> examples; // 示例/案例
    private String quoteText;     // 名人名言（可选）
    private String quoteAuthor;   // 名言作者
    private List<QuizQuestion> quizQuestions; // 随堂测验
    private String activityDesc;  // 课堂活动描述
    private Integer duration;     // 建议时长（分钟）
    private String teacherNote;    // 教师备注（Speaker Notes）
    private String visualGuidance; // 视觉建议（"流程图"、"表格"）
}
```

### 4.2 ChapterContent（AI 充实后的章节内容）

```java
@Data
@Builder
public class ChapterContent {
    private Long chapterId;
    private String chapterTitle;
    private String learningObjectives;    // 学习目标
    private List<SlideContent> slides;    // 该章节所有幻灯片
    private int totalDuration;             // 该章节总时长（分钟）
}
```

### 4.3 PptGenerationResult（生成结果）

```java
@Data
@Builder
public class PptGenerationResult {
    private TeachingMaterial material;     // 课件记录
    private List<ChapterContent> chapters; // 详细内容
    private int totalSlides;              // 总幻灯片数
    private int totalDuration;            // 总时长（分钟）
}
```

### 4.4 现有模型扩展：PptGenerateRequest.SlideData

```java
public static class SlideData {
    private String type;    // 新增: text|diagram|case|quote|quiz|activity|reflection
    private String title;
    private String content;
    private List<String> contentList;
    private List<String> keyPoints;
    private List<String> questions;        // 新增: 测验题
    private List<String> points;
    private String imagePath;
    private Integer duration;
    // 新增字段
    private String mainBody;               // 主要段落文字
    private List<String> examples;         // 案例列表
    private String quoteText;              // 名言内容
    private String quoteAuthor;            // 名言作者
    private String teacherNote;            // 教师备注
    private String visualGuidance;         // 视觉建议
}
```

---

## 5. AI 充实流程

### 5.1 AIEnrichmentService 接口

```java
public interface AIEnrichmentService {
    /**
     * 为课程的所有章节生成详细内容
     * @param course  课程实体（含 outline JSON）
     * @param style   PPT 风格（影响提示词微调）
     * @return 每个章节的详细内容
     */
    List<ChapterContent> enrichChapters(Course course, String style);
}
```

### 5.2 提示词设计

每个 Chapter 调用一次 LLM，输入课程上下文 + Chapter 元数据，输出该章节的完整幻灯片内容：

```
【系统提示】
你是一位资深教育专家，精通课程设计和教学幻灯片制作。
根据提供的章节信息，为每个章节设计详细的教学幻灯片内容。

【章节信息】
- 课程标题：{courseTitle}
- 课程描述：{courseDescription}
- 章节标题：{chapterTitle}
- 章节时长：{chapterDuration} 分钟
- 关键知识点：{keyPoints}

【输出格式 - JSON】
{
  "chapterTitle": "...",
  "learningObjectives": ["目标1", "目标2", "目标3"],
  "slides": [
    {
      "slideType": "text",
      "title": "幻灯片标题",
      "mainBody": "主要阐述文字，100-300字，内容深入浅出，包含具体例子",
      "bullets": ["要点1", "要点2", "要点3"],
      "duration": 3,
      "teacherNote": "教师备注：讲解时的注意事项",
      "visualGuidance": "流程图"
    },
    {
      "slideType": "case",
      "title": "案例分析",
      "mainBody": "案例背景和描述",
      "examples": ["案例1详解", "案例2详解"],
      "duration": 4,
      "teacherNote": "可分组讨论",
      "visualGuidance": "图片+文字"
    },
    {
      "slideType": "quiz",
      "title": "随堂测验",
      "quizQuestions": [
        {
          "type": "choice",
          "question": "题目...",
          "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
          "answer": "A",
          "explanation": "解析..."
        }
      ],
      "duration": 2
    },
    {
      "slideType": "reflection",
      "title": "思考与讨论",
      "mainBody": "讨论题内容",
      "duration": 2,
      "teacherNote": "可举手发言或分组讨论"
    }
  ],
  "totalDuration": 15
}
```

### 5.3 幻灯片类型覆盖

| 顺序 | slideType | 说明 | 占比 |
|------|-----------|------|------|
| 1 | text | 核心概念阐述 | 40% |
| 2 | diagram | 图表/流程/对比 | 15% |
| 3 | case | 案例分析 | 15% |
| 4 | quote | 名人名言 + 解读 | 5% |
| 5 | quiz | 随堂测验（每章1-2道） | 10% |
| 6 | activity | 课堂活动 | 10% |
| 7 | reflection | 思考讨论题 | 5% |

---

## 6. PPT 构建流程（重构）

```
输入：List<ChapterContent>
         │
         ▼
PptBuildService.buildPptRequest()
         │
         ▼
    遍历每个 ChapterContent
         │
         ├─ Chapter Header Slide (已有: chapter)
         │
         ├─ SlideContent[0..n] → SlideData
         │    根据 slideType 映射到 JS 渲染函数
         │
         └─ Chapter Summary Slide (已有: summary)
         │
         ▼
输出：PptGenerateRequest
         │
         ▼
    PptGeneratorClient.generate()
         │
         ▼
    generate-ppt.js → PPTX file
         │
         ▼
    FileStorageService.upload()
         │
         ▼
    TeachingMaterialService.save()
```

---

## 7. 幻灯片类型 → JS 渲染函数映射

| slideType | JS 函数 | 布局特点 |
|-----------|---------|---------|
| text | `createTextSlide()` | 标题 + 正文段落 + 要点列表 |
| diagram | `createDiagramSlide()` | 左右分栏（文字+图示占位） |
| case | `createCaseSlide()` | 案例标题 + 背景 + 分析要点 |
| quote | `createQuoteSlide()` | 大字名言居中 + 作者 + 解读 |
| quiz | `createQuizSlide()` | 题目 + 选项列表 |
| activity | `createActivitySlide()` | 活动标题 + 步骤列表 |
| reflection | `createReflectionSlide()` | 问题突出显示 + 引导文字 |
| title | `createTitleSlide()` | （已有）封面 |
| chapter | `createChapterSlide()` | （已有）章节封面 |
| summary | `createSummarySlide()` | （已有）章节小结 |
| end | `createEndSlide()` | （已有）结束页 |

---

## 8. API 设计

```
POST /api/material/ppt/generate
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "courseId": 1,               // 必填：关联课程
  "template": "elegant",       // 可选：default | elegant | minimal | vibrant
  "style": "academic",         // 可选：academic(学术) | vivid(生动) | concise(精炼)
  "includeQuiz": true,         // 可选：是否包含测验页（默认 true）
  "includeActivity": true,    // 可选：是否包含活动页（默认 true）
  "slidesPerChapter": 5       // 可选：每章幻灯片数量（默认 4-6 自动）
}

Response:
{
  "code": 200,
  "data": {
    "materialId": 123,
    "fileUrl": "https://minio/.../xxx.pptx",
    "totalSlides": 28,
    "totalDuration": 45,
    "chapters": [
      {
        "chapterId": 1,
        "title": "电磁感应基础",
        "slideCount": 6,
        "slides": [
          { "type": "text", "title": "...", "duration": 3 },
          { "type": "quiz", "title": "...", "questionCount": 2 }
        ]
      }
    ]
  }
}
```

---

## 9. 模板系统扩展

在现有 4 套模板基础上扩展：

```javascript
const TEMPLATES = {
  default:  { name: '学院蓝',   colors: {...}, fontTitle: 'Microsoft YaHei', fontBody: 'SimSun' },
  elegant:  { name: '典雅绿',   colors: {...}, fontTitle: 'SimHei', fontBody: 'Microsoft YaHei' },
  minimal:  { name: '简约白',   colors: {...}, fontTitle: 'Helvetica Neue', fontBody: 'Arial' },
  vibrant:  { name: '活力橙',   colors: {...}, fontTitle: 'PingFang SC', fontBody: 'Microsoft YaHei' },
  academic: { name: '学术风',   colors: { primary: '1A3A5C', secondary: '2E6B8A', ... },
              fontTitle: 'Georgia', fontBody: 'Times New Roman' },
}
```

---

## 10. 异步化改造（可选）

当前 PPT 生成是同步的，文件较大时可能超时：

```
POST /api/material/ppt/generate
  → 返回 taskId（202 Accepted）
  → 前端轮询 GET /api/material/task/{taskId}

AsyncTask 表状态：PENDING → PROCESSING → COMPLETED / FAILED
```

---

## 11. 实现计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| Phase 1 | 扩展 JS 层：新增 7 种幻灯片渲染函数 | P0 |
| Phase 2 | 新增 AIEnrichmentService + Mock 实现 | P0 |
| Phase 3 | 重构 PptBuildService，集成 AIEnrichmentService | P0 |
| Phase 4 | 扩展 PptGenerateRequest.SlideData 字段 | P1 |
| Phase 5 | API 参数扩展（style/includeQuiz 等） | P1 |
| Phase 6 | 异步化改造（AsyncTask + 轮询） | P2 |
| Phase 7 | 模板系统扩展（academic 等新模板） | P2 |

---

## 12. 测试验证

| 级别 | 内容 |
|------|------|
| 单元测试 | `AIEnrichmentService` — mock LLM provider，验证 JSON 解析正确 |
| 单元测试 | `PptBuildService` — 验证 ChapterContent → PptGenerateRequest 映射 |
| 集成测试 | 调用 `/api/material/ppt/generate`，验证 PPTX 文件可打开、内容非空 |
| 集成测试 | 调用 `/api/material/{id}`，验证 TeachingMaterial 记录正确 |
