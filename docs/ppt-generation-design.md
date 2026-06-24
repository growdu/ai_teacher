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

**新问题（趣味性 & 生动性专项）**：

| # | 问题 | 影响 |
|---|------|------|
| 6 | 大量文字段落，缺乏视觉节奏 | 学生注意力涣散 |
| 7 | 只有静态文本，无图表/图示/动画 | 知识密度低、记忆点少 |
| 8 | 模板仅有 4 套（蓝/绿/白/橙），风格雷同 | 审美疲劳 |
| 9 | 教学元素（漫画/信息图/流程动画）缺失 | 趣味性不足 |
| 10 | 知识点堆砌，无知识结构可视化 | 学习路径不清晰 |
| 11 | **没有互动性设计**，整堂课单向灌输 | 学生参与度低、注意力易涣散 |
| 12 | 老师无法实时了解学生掌握情况 | 教学效果无法评估 |

---

## 2. 对知识点模块的要求

> PPT 模块不直接依赖 `KnowledgePoint` 表，只通过 `Course.outline` 中转数据。但 **Course.outline 的生成质量直接取决于知识点的内容质量**，因此两者之间存在隐式依赖关系。

### 2.1 知识点 → 课程的输入契约

`CourseGenerateService.generateCourse()` 调用 `KnowledgePointService.getInputForCourse(kpId)`，获得标准化知识点输入：

```java
public class KnowledgePointInput {
    Long knowledgePointId;
    String subjectType;           // science / humanities（必填）
    String subject;              // 数学/物理/化学/语文/历史（必填）
    String grade;                // 年级（必填，影响内容深度）
    String content;              // 原始内容（LLM 输入）
    String contentStructured;    // JSON，结构化内容（必填）
    // {
    //   "type": "concept|formula|principle|example",
    //   "coreDefinition": "核心定义（必填）",
    //   "keyPoints": ["要点1", "要点2"],         // 至少3条
    //   "formulas": ["公式1", "公式2"],          // 理科必填
    //   "examples": ["例题1", "例题2"],          // 建议至少1个
    //   "commonMistakes": ["易错点1"]             // 建议至少1个
    // }
    List<String> keyPoints;      // 核心要点列表（从contentStructured提取）
    List<String> formulas;       // 公式列表（理科）
    List<String> prerequisiteTitles; // 前置知识点标题
    Integer estimatedMinutes;    // 预估讲解时长（建议提供）
}
```

### 2.2 知识点内容质量对 PPT 的影响

| 知识点字段 | PPT 生成的影响 | 最低要求 |
|-----------|-------------|---------|
| `contentStructured.type` | 决定幻灯片类型选择（concept→concept slide，example→exercise slide） | 必填 |
| `coreDefinition` | concept slide 的主要内容来源 | 必填 |
| `keyPoints` | bullet slide / infographic slide 的要点来源 | 至少3条 |
| `formulas` | derivation / concept slide 的公式来源（理科） | 理科必填 |
| `examples` | exercise slide 的例题来源 | 建议提供 |
| `commonMistakes` | 讨论/辨析类幻灯片的内容来源 | 建议提供 |
| `estimatedMinutes` | 决定幻灯片总时长分配 | 建议提供 |

### 2.3 知识点质量等级与 PPT 效果

| 等级 | 知识点内容质量 | PPT 生成效果 |
|------|-------------|------------|
| A（优秀） | 完整提供 `contentStructured`（type + coreDefinition + keyPoints + formulas + examples + commonMistakes） | 幻灯片丰富、准确、互动节点充足 |
| B（合格） | 提供 `contentStructured` 但缺少部分字段（仅有 keyPoints） | 幻灯片基本可用，但例题/易错点缺失 |
| C（不足） | 只有纯文本 `content`，无结构化信息 | 幻灯片依赖 LLM 自由发挥，质量不稳定 |
| D（不合格） | `content` 过于简短（少于50字） | PPT 内容空洞，不可用 |

**CourseGenerateService 应在生成课程前评估知识点质量，等级 C/D 时向用户发出警告**。

### 2.4 API 层要求

`POST /api/course/generate` 接口应在生成课程前**校验知识点输入**：

```json
// 请求示例
{
  "knowledgePointId": 1,
  "generateAfter": ["ppt", "video", "quiz"]
}

// 响应（知识点质量不足时）
{
  "code": 400,
  "message": "知识点内容不足以保证PPT质量",
  "data": {
    "knowledgePointId": 1,
    "qualityGrade": "C",
    "warnings": [
      "缺少 contentStructured 字段，内容结构化程度不足",
      "缺少 formulas 字段，理科课程建议补充公式",
      "缺少 examples 字段，建议提供至少1个例题"
    ],
    "suggestion": "请先完善知识点内容，或选择'强制生成'跳过质量检查"
  }
}
```

---

## 3. 目标

**新增核心目标**：

- **趣味性**：引入知识漫画、故事线、互动问答等趣味教学元素
- **生动性**：用图表、信息图、流程动画代替大段文字
- **格式多样化**：多种模板 + 多种教学幻灯片类型
- **减少文字密度**：每张幻灯片不超过 3 行文字，以视觉元素承载信息
- **强互动性**：每节课设计 3-5 个师生互动节点（提问/投票/讨论/实验），实时反馈学习效果

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
│                quiz | activity | reflection |      │
│                comic | infographic | timeline      │
└────────────────────────────────────────────────────┘
```

---

## 4. 核心数据模型

### 4.1 SlideContent（AI 充实后的幻灯片内容）

```java
@Data
@Builder
public class SlideContent {
    private String slideType;    // text | diagram | case | quote | quiz | activity | reflection | comic | infographic | timeline | problem | concept | exercise | derivation | vote | poll | quick-fire | experiment | game | exit-ticket | result-viz
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
    private String visualGuidance; // 视觉建议（"流程图"、"表格"、"漫画"、"信息图"）
    private String layout;         // 布局类型：bento | two-col | full-bleed | grid | hub-spoke（来自 baoyu-infographic）
    private String artStyle;       // 美术风格：hand-drawn | chalkboard | corporate-memphis | vintage | kawaii（来自 baoyu-comic）
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
    private String narrativeArc;          // 新增：章节叙事弧（故事线，用于趣味性）
    private String visualTheme;           // 新增：该章节视觉主题（影响 infographic/art style 选择）
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
    private List<String> assetUrls;      // 新增：生成的图片/漫画/信息图 URL（MinIO）
}
```

### 4.4 现有模型扩展：PptGenerateRequest.SlideData

```java
public static class SlideData {
    private String type;    // 新增: text|diagram|case|quote|quiz|activity|reflection|comic|infographic|timeline|problem|concept|exercise|derivation|vote|poll|quick-fire|experiment|game|exit-ticket|result-viz
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
    private String layout;                 // 布局：bento | two-col | full-bleed | grid | hub-spoke
    private String artStyle;               // 美术风格：hand-drawn | chalkboard | corporate-memphis | vintage
    private String assetUrl;               // 该幻灯片关联的生成图片/信息图 URL
}
```

---

## 5. AI 充实流程（增强版）

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

### 5.2 提示词设计（趣味性 & 生动性增强版）

#### 5.2.1 文科 vs 理科差异化策略

> **理科（数学/物理/化学）必须保证准确性优先**，不能为了趣味性牺牲知识正确性。

| 维度 | 文科（历史/语文/政治） | 理科（数学/物理/化学） |
|------|----------------------|----------------------|
| 首要原则 | 趣味性 + 价值观引导 | **准确性 + 逻辑严谨** |
| 叙事弧 | 故事线（人物/事件/情感） | **问题驱动弧**（问题→探究→结论→应用） |
| 知识呈现 | 叙述为主，信息图为辅 | **概念图+公式+例题** 三位一体 |
| 测验比重 | 10% | **20-30%**（每节必有随堂练习） |
| 错误设计 | 案例警示 | **故意设错→学生辨析→正确理解** |
| 趣味元素 | 故事/漫画/名言 | **实验模拟/推导动画/生活中的现象** |
| AI 提示词 | 允许适当的文学化表达 | **严格禁止比喻掩盖定义；公式必须有单位** |

#### 5.2.2 理科「问题驱动四步法」幻灯片模板

理科每节内容必须严格遵循以下四步结构，**不可跳过任意一步**：

```
┌────────────────────────────────────────────────────────────┐
│  步骤一：问题引入（Problem Introduction）                    │
│  ─────────────────────────────────────────────────────────  │
│  用一个生活中的真实问题/现象引入，激发好奇心                 │
│  slideType: "problem" | 时长: 2min | 文字: ≤3行            │
│                                                              │
│  典型模板：                                                  │
│  "一个足球被踢出后，它的轨迹是什么样的？"                   │
│  "如何测量一栋大楼的高度？"                                  │
│  "为什么天空是蓝色的？"                                      │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│  步骤二：概念讲解（Concept Teaching）                        │
│  ─────────────────────────────────────────────────────────  │
│  揭示原理/公式/定理，配合概念图和公式推导                     │
│  slideType: "concept" | 时长: 5-8min | 文字: ≤3行          │
│                                                              │
│  典型模板：                                                  │
│  [概念图/流程图] + [核心公式] + [关键要点3条]               │
│  严禁用比喻替代定义                                          │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│  步骤三：例题演练（Practice & Exercise）                     │
│  ─────────────────────────────────────────────────────────  │
│  先示范1道典型例题，再给出2-3道变式训练题                   │
│  slideType: "exercise" | 时长: 5-8min | 题目必须有完整解答   │
│                                                              │
│  典型模板：                                                  │
│  例题展示 → 分步解答 → 学生思考（留白）→ 公布答案           │
│  变式题：基础题1道 + 中等题1道 + 挑战题1道                  │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│  步骤四：总结提升（Summary & Reflection）                     │
│  ─────────────────────────────────────────────────────────  │
│  用一句话总结核心知识点 + 知识结构图 + 课后思考题             │
│  slideType: "summary" | 时长: 2min | 文字: ≤3行             │
│                                                              │
│  典型模板：                                                  │
│  [一句话总结] + [知识结构图/思维导图] + [课后挑战题]        │
└────────────────────────────────────────────────────────────┘
```

#### 5.2.3 理科新增 slideType

| slideType | 说明 | 关键字段 |
|-----------|------|---------|
| `problem` | 问题引入页 | `question`, `scenario`, `imagePrompt` |
| `concept` | 概念讲解页 | `formula`, `diagramType`, `keyPoints` |
| `exercise` | 例题演练页 | `exampleProblem`, `solution`, `variants[]` |
| `derivation` | 推导/证明页 | `stepByStep[]`, `formula` |

#### 5.2.4 理科准确性保障机制

```java
// AIEnrichmentService.enrichChapters() 中新增校验逻辑
public class AccuracyValidator {
    // 1. 公式必须包含单位
    // 2. 数学推导每步必须有逻辑依据
    // 3. 化学方程式必须配平
    // 4. 物理单位必须统一（SI制）
    // 5. 数值答案必须带有效数字
}
```

**AIEnrichmentService 理科模式提示词片段**：

```
【准确性要求 - 理科专属】
- 公式必须写完整，包含所有符号和单位，不得使用"↑↓"省略
- 化学方程式必须配平，电荷守恒
- 数学证明每步必须有依据，不能跳步
- 所有数据必须标注来源和测量条件
- 例题答案必须给出完整解题过程，不得只给结果
- 严禁：口语化类比替代正式定义（如"电子很小"→应写"电子半径约2.8×10⁻¹⁵m"）
- 允许：生活现象引入 + 可视化动画描述原理

【问题驱动四步法 - 必须执行】
Step 1 问题引入：用一个可操作的生活问题/实验现象引入（≤3行文字）
Step 2 概念讲解：揭示核心原理/公式，配合概念图（严禁跳步）
Step 3 例题演练：典型例题→分步解答→变式训练（题目必须有完整解答）
Step 4 总结提升：一句话核心结论 + 知识结构图 + 课后思考题
```

#### 5.2.5 文科模式提示词片段

```
【趣味性要求 - 文科专属】
- 优先使用故事线：人物经历/历史事件/文学案例
- 允许文学化表达和情感共鸣设计
- 名人名言占比可提高至10%
- 漫画/插图用于情感共鸣，不强制要求每页都有
- 允许适当的比喻和拟人（但定义仍需严谨）
```

#### 5.2.6 通用提示词设计

**每个 Chapter 调用一次 LLM**，输入课程上下文 + Chapter 元数据：

```
【系统提示】
你是一位资深教育专家，精通课程设计和教学幻灯片制作。
你的核心理念：减少文字，增加视觉。每张幻灯片只传达一个核心信息。
擅长使用信息图、知识漫画、流程图、对比图来代替大段文字描述。
善于设计趣味性的学习路径：通过故事线、案例、思考题激发学生兴趣。
科目类型：{subjectType}（文科/理科，由调用方传入）
科目：{courseTitle}

【章节信息】
- 课程标题：{courseTitle}
- 课程描述：{courseDescription}
- 章节标题：{chapterTitle}
- 章节时长：{chapterDuration} 分钟
- 关键知识点：{keyPoints}
- 目标受众：{audience}（中学生/大学生/成人）

【叙事弧设计要求】
在生成幻灯片之前，先为此章节设计一个"叙事弧"：
- 开场问题：用一个生活中的真实问题引入
- 探索阶段：引导学生逐步理解概念
- 发现时刻：核心原理/规律的揭示
- 应用阶段：知识在实际中的应用

【幻灯片设计原则】
1. 每张幻灯片不超过 3 行文字（每行不超过 15 字）
2. 优先使用图表、漫画、信息图来表达概念
3. 文字用于引导，视觉用于解释
4. 每个知识点至少配一个视觉元素
5. 趣味元素占比不低于 20%（漫画/故事/名言/思考题）

【输出格式 - JSON】
{
  "chapterTitle": "...",
  "narrativeArc": {
    "opening": "开场问题（生活情境）",
    "exploration": "探索阶段描述",
    "discovery": "发现时刻描述",
    "application": "应用阶段描述"
  },
  "learningObjectives": ["目标1", "目标2", "目标3"],
  "slides": [
    {
      "slideType": "infographic",
      "title": "知识点的名字",
      "visualGuidance": "hub-spoke",     // 来自 baoyu-infographic 布局系统
      "artStyle": "hand-drawn-edu",      // 来自 baoyu-infographic 风格系统
      "bullets": ["要点1", "要点2"],
      "duration": 2,
      "teacherNote": "教师备注"
    },
    {
      "slideType": "comic",
      "title": "概念理解漫画（2-4格）",
      "visualGuidance": "ligne-claire",  // 线稿清晰风格，适合教育漫画
      "comicPanels": [
        { "scene": "场景描述", "dialogue": "角色对话（可选）", "caption": "旁白说明" },
        { "scene": "...", "caption": "..." }
      ],
      "duration": 3,
      "teacherNote": "可让学生分角色朗读对话"
    },
    {
      "slideType": "diagram",
      "title": "流程图：XXX的步骤",
      "visualGuidance": "linear-progression",
      "diagramType": "flowchart | timeline | comparison | hierarchy",
      "bullets": ["步骤1", "步骤2", "步骤3"],
      "duration": 3
    },
    {
      "slideType": "case",
      "title": "生活中的案例",
      "mainBody": "案例描述（1-2句话）",
      "examples": ["案例1", "案例2"],
      "duration": 4,
      "teacherNote": "可分组讨论"
    },
    {
      "slideType": "quote",
      "title": "名人名言",
      "quoteText": "名言内容",
      "quoteAuthor": "作者",
      "duration": 1
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
  "totalDuration": 20
}
```

### 5.3 幻灯片类型覆盖（增强版）

> **文字密度控制**：视觉型幻灯片（infographic + diagram + comic + concept）占比 ≥ 55%，纯文字型（text + reflection）≤ 20%。

#### 5.3.1 通用类型

| 顺序 | slideType | 说明 | 占比 | 视觉来源 |
|------|-----------|------|------|---------|
| 1 | infographic | 知识结构可视化 | 25% | baoyu-infographic |
| 2 | comic | 概念理解漫画 | 15% | baoyu-comic |
| 3 | diagram | 流程图/对比图/时间线 | 15% | PptxGenJS |
| 4 | case | 生活案例分析 | 15% | 文字+图示 |
| 5 | quote | 名人名言 | 5% | 装饰文字 |
| 6 | quiz | 随堂测验 | 10% | PptxGenJS |
| 7 | reflection | 思考讨论题 | 5% | 引导文字 |
| 8 | activity | 课堂互动活动 | 5% | 步骤图示 |
| 9 | timeline | 历史/发展时间线 | 5% | PptxGenJS |

#### 5.3.2 理科专属类型

| 顺序 | slideType | 说明 | 占比 | 出现位置 |
|------|-----------|------|------|---------|
| R1 | problem | 问题引入页 | 每章 1 次 | 章节第 1 张 |
| R2 | concept | 概念/公式讲解页 | 每章 2-4 次 | 四步法第 2 步 |
| R3 | derivation | 推导/证明页 | 数学/物理必选 | 四步法第 2 步 |
| R4 | exercise | 例题+变式题演练 | 每章 2-3 次 | 四步法第 3 步 |
| R5 | summary | 章节总结 | 每章 1 次 | 四步法第 4 步 |

---


### 5.X 互动性设计体系

> 老师的 PPT 不是"演讲稿"，而是"教学工具"。每节课必须在关键节点设计师生互动，让学生的注意力始终跟着老师的节奏走。

#### 5.X.1 互动性设计原则

| 原则 | 说明 | 实施要求 |
|------|------|---------|
| **每课至少 3 个互动节点** | 导入、概念讲解后、练习后必须各有 1 次 | AIEnrichmentService 生成时强制插入 |
| **互动形式匹配教学内容** | 投票→概念辨析；讨论→开放性问题；练习→知识检测 | 由 slideType 决定互动类型 |
| **结果即时反馈** | 互动结果必须可视化（柱状图/词云）展示给学生 | 依赖后端实时推送能力 |
| **老师有"暂停权"** | 互动幻灯片上有"老师控制"按钮，老师决定何时开始 | 前端/PPT 交互层支持 |
| **分层互动** | 简单题→全班投票；开放题→小组讨论；难题→个人思考后展示 | AI 提示词按难度分配互动类型 |

#### 5.X.2 互动节点类型分类

| 互动类型 | slideType | 触发时机 | 持续时间 | 后端依赖 |
|---------|-----------|---------|---------|---------|
| `vote` 课堂投票 | 全班选择 A/B/C/D | 概念辨析后 | 1-2min | 实时结果聚合 |
| `poll` 快速统计 | 全班选择程度/感受 | 课程中段 | 30s | 实时结果聚合 |
| `think-pair-share` 思考配对分享 | 个人思考→同桌讨论→全班 | 开放性问题后 | 3-5min | 无（课堂组织） |
| `quick-fire` 快速问答 | 学生举手/抢答 | 知识回忆阶段 | 1min | 无 |
| `experiment` 分组实验 | 动手操作→观察记录 | 物理/化学/生物 | 10-15min | 实验记录 |
| `game` 知识游戏 | 竞赛/PK 形式 | 复习课 | 5-10min | 计分系统 |
| `exit-ticket` 出门票 | 回答 1 个问题后离开 | 课程结尾 | 2min | 结果收集 |

#### 5.X.3 互动幻灯片 JS 渲染函数

| slideType | JS 函数 | 布局特点 |
|-----------|---------|---------|
| `vote` | `createVoteSlide()` | 大字题目居中 + A/B/C/D 选项卡片，选项带颜色 |
| `poll` | `createPollSlide()` | 标题 + 滑动条/程度选择条 |
| `quick-fire` | `createQuickFireSlide()` | 倒计时 + 大字问题 + 抢答提示 |
| `experiment` | `createExperimentSlide()` | 实验步骤图 + 记录表格占位 |
| `game` | `createGameSlide()` | 分数板 + 题目卡片 + 计时器占位 |
| `exit-ticket` | `createExitTicketSlide()` | 问题 + 空白回答区 |

#### 5.X.4 实时反馈机制（后端设计）

**架构**：

```
老师 PPT 端（投影屏幕）
    │
    │ WebSocket 连接（roomId = 课堂ID）
    ▼
┌──────────────────────────────────────────┐
│  InteractionService (Java WebSocket)      │
│                                           │
│  每种互动类型有独立处理器：               │
│  - VoteHandler    → 聚合选项计数         │
│  - PollHandler    → 聚合程度分布          │
│  - QuickFireHandler → 记录抢答时间       │
│  - ExitTicketHandler → 收集答题结果       │
│                                           │
│  推送结果给老师端 + 学生端（可选）        │
└──────────────────────────────────────────┘
    │
    │ 结果数据（JSON）
    ▼
老师端：实时显示柱状图/统计结果
学生端：显示个人提交状态/正确答案
```

**后端核心接口**：

```java
// 发起互动
@MessageMapping("/interaction/start")
public void startInteraction(InteractionRequest req) {
    // 创建互动房间，广播开始信号
}

// 学生提交回答
@MessageMapping("/interaction/submit")
public void submitAnswer(AnswerSubmission submission) {
    // 聚合答案，实时计算统计结果
}

// 获取实时结果（轮询备选）
@GetMapping("/api/interaction/{interactionId}/results")
public ApiResponse<InteractionResult> getResults(String interactionId) {
    // 返回当前聚合结果
}
```

**结果展示幻灯片类型**：`result-viz`（柱状图/饼图/词云）

```javascript
function createResultVizSlide() {
    // 接收实时数据，渲染柱状图/饼图
    // 显示正确答案高亮 + 全班正确率
}
```

#### 5.X.5 互动性 × 学科适配

| 学科 | 推荐互动类型 | 理由 |
|------|------------|------|
| 数学 | vote（概念辨析）、quick-fire（公式记忆）、exit-ticket | 数学需要即时验证理解 |
| 物理 | experiment、vote（判断对错）、think-pair-share | 物理需要观察和讨论 |
| 化学 | experiment、poll（安全程度）、game | 化学需要实验操作 |
| 历史 | think-pair-share、poll（态度选择）、exit-ticket | 历史适合开放讨论 |
| 语文 | think-pair-share、game（背诵PK）、exit-ticket | 语文需要表达和积累 |

#### 5.X.6 AI 提示词中的互动设计指令

AIEnrichmentService 在生成章节幻灯片时，必须在叙事弧中嵌入互动节点：

```
【互动设计要求】
在设计章节叙事弧时，必须在以下三个位置各插入至少1个互动节点：
1. 导入环节（开场问题之后）：插入 vote 或 poll（检验预习效果）
2. 概念讲解后（揭示原理之后）：插入 quick-fire 或 think-pair-share（检验即时理解）
3. 练习结束后（变式题之后）：插入 exit-ticket 或 game（检验知识掌握）

【互动节点格式】
每个互动节点必须包含：
- "interactionType": "vote | poll | quick-fire | experiment | think-pair-share | game | exit-ticket"
- "triggerMoment": "概念讲解后" | "练习结束后" | "课程导入时"
- "question": "互动的问题内容"
- "expectedDuration": 2  // 分钟
- "teacherNote": "老师操作提示（如：点击开始投票）"
```

---

## 6. PPT 生成 LLM 选型调研

> AIEnrichmentService 是 PPT 生成质量的核心瓶颈。本章节调研适合"生成结构化幻灯片内容 + 教学设计"的 LLM，评估维度：中文教育内容质量、JSON 结构化输出稳定性、多模态（图片生成）、成本。

### 6.0 评估维度与权重

| 评估维度 | 权重 | 说明 |
|---------|------|------|
| 中文教育内容质量 | 30% | 理科准确性、文科趣味性、知识深度 |
| JSON 结构化输出稳定性 | 25% | SlideContent JSON 解析成功率 |
| 工具调用能力（Function Calling） | 20% | 能否稳定调用图片生成等工具 |
| 多模态（图片/图表生成） | 15% | 原生支持图片生成或调用外部工具 |
| 成本 | 10% | 每千次章节调用成本 |

### 6.1 LLM 横向对比

| LLM | 厂商 | 中文质量 | JSON 稳定性 | Function Calling | 多模态 | 上下文 | 成本/吨 |
|------|------|---------|------------|-----------------|--------|-------|--------|
| GPT-4o | OpenAI | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐（DALL-E） | 128k | $2.5 |
| GPT-4o-mini | OpenAI | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐（DALL-E） | 128k | $0.15 |
| Claude 3.5 Sonnet | Anthropic | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐（仅视觉） | 200k | $3.0 |
| Claude 3.5 Haiku | Anthropic | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | 200k | $0.25 |
| Gemini 2.0 Flash | Google | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐（原生图） | 1M | $0.10 |
| Gemini 2.5 Pro | Google | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐（原生图） | 1M | $1.25 |
| DeepSeek V3 | 深度求索 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | 64k | $0.50 |
| GLM-4 | 智谱 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 128k | $0.50 |
| Qwen 2.5 72B | 阿里 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 32k | $0.90 |
| MiniMax | MiniMax | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐（视频） | 100k | $0.20 |

### 6.2 场景化推荐

#### 场景 A：高质量生产环境（大学/培训机构）

**推荐：GPT-4o + GPT-4o-mini（分级策略）**

| 内容类型 | 使用模型 | 原因 |
|---------|---------|------|
| 课程大纲生成 | GPT-4o | 强推理 + 结构化输出，一次性生成完整 outline |
| 章节幻灯片内容（文字） | GPT-4o-mini | 量大、JSON 稳定、成本低 |
| 信息图生成 prompt | GPT-4o | 提示词质量决定图片质量 |
| 知识漫画分镜 | GPT-4o | 需要强角色一致性提示词 |
| 随堂测验生成 | GPT-4o-mini | 格式固定、量大 |

分级策略成本估算：
- 1 门课（5 章，每章 8 张幻灯片）= 40 次 min 调用 + 5 次大调用
- 成本：40 × $0.15 + 5 × $2.5 = **$14.5/门课**

#### 场景 B：中文教育主力（中小学）

**推荐：GLM-4 + 智谱 ChatGLM（中文专项）**

| 内容类型 | 使用模型 | 原因 |
|---------|---------|------|
| 通用幻灯片内容 | GLM-4 | 中文质量接近 GPT-4o，成本低 |
| 理科内容（公式/推导） | GLM-4 | 智谱对中文理科术语处理较好 |
| 图片生成 | Gemini 2.0 Flash（via API） | 性价比最高，支持中文 prompt |

**备选：DeepSeek V3**
- 数学推导能力强，适合物理/化学
- 缺点：Function Calling 支持较新，需要充分测试

#### 场景 C：成本敏感场景（个人老师/小机构）

**推荐：GPT-4o-mini + Gemini 2.0 Flash**

- 主模型：GPT-4o-mini（JSON 内容生成）
- 图片模型：Gemini 2.0 Flash（原生图生成，成本 $0.10/吨）
- 成本估算：$0.25/千章节 ≈ 可忽略不计

#### 场景 D：离线/私有部署

**推荐：Qwen 2.5 + Ollama 本地推理**

- Qwen 2.5 72B：通过 Ollama 本地部署
- 优点：数据不出本地，适合有隐私要求的机构
- 缺点：JSON 稳定性不如 GPT-4o，需要后处理校验

### 6.3 图像生成模型专项对比

PPT 中的信息图/漫画依赖图像生成质量：

| 模型 | 适合风格 | 中文支持 | API 可用性 | 成本 |
|------|---------|---------|-----------|------|
| DALL-E 3 | 写实/插画 | ⭐⭐⭐⭐ | OpenAI API | $0.04/图 |
| Midjourney V6 | 高质量插画/艺术 | ⭐⭐⭐ | 需第三方中介 | $0.03/图 |
| Stable Diffusion 3 | 定制化/可控 | ⭐⭐⭐ | 本地部署 | GPU 成本 |
| Gemini 2.0 Flash | 快速草图/示意图 | ⭐⭐⭐⭐ | Google API | $0.10/1M token |
| FLUX.1 | 精确文字渲染 | ⭐⭐⭐ | Replicate API | $0.05/图 |
| 通义万相 | 中文艺术/插画 | ⭐⭐⭐⭐⭐ | 阿里云 | ¥0.16/图 |
| 文心一格 | 国风/中文 | ⭐⭐⭐⭐⭐ | 百度云 | ¥0.25/图 |

**推荐组合（中文教育 PPT）**：

| 用途 | 推荐模型 | 原因 |
|------|---------|------|
| 理科示意图（流程图/电路图/化学式） | DALL-E 3 或 FLUX.1 | 精确文字渲染能力强 |
| 知识漫画（分镜/人物） | Midjourney V6 或 DALL-E 3 | 风格一致性与艺术感 |
| 信息图（卡片/对比/时间线） | 通义万相 或 Gemini 2.0 Flash | 中文友好，性价比高 |
| 国风/传统题材（语文/历史） | 文心一格 | 中文艺术风格最优 |
| 黑白板书风插图 | Stable Diffusion 3（本地） | 可控风格，低成本 |

### 6.4 提示词工程最佳实践

#### 6.4.1 JSON 输出稳定性技巧

LLM 直接输出 SlideContent JSON 时容易出现：
- 混入 Markdown 代码块（\`\`\`json...）
- 字段名拼写不一致
- 数组元素漏逗号

**解决策略 1：Function Calling（推荐）**

```java
// 使用 GPT-4o / Claude 的 Function Calling，强制 JSON Schema 输出
@JsonFunction(
    name = "generate_slides",
    description = "生成课程章节的幻灯片内容",
    parameters = {
        @Property(name = "slides", type = "array"),
        @Property(name = "narrativeArc", type = "object"),
        @Property(name = "learningObjectives", type = "array")
    }
)
```

**解决策略 2：结构化输出提示词模板**

直接输出 JSON，不要有任何前缀或解释。输出格式必须严格遵守：
{"slides": [...], "narrativeArc": {...}, "learningObjectives": [...]}
不允许出现： markdown 代码块包裹、以上是、下面是的解释性文字。
如违反格式，本次回答将被判定为失败。

**解决策略 3：后置校验 + 自动修复**

```java
// AIEnrichmentService.enrichChapters() 中
String raw = llm.complete(prompt);
// 1. 去除 Markdown 代码块包装
raw = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
// 2. JSON Schema 校验（用 Jackson）
try {
    JsonNode node = objectMapper.readTree(raw);
    validateSlideContent(node);
} catch (JsonProcessingException e) {
    // 3. 自动重试一次
    raw = llm.complete(retryPrompt);
}
```

#### 6.4.2 提示词模板（完整版）

```
【系统提示】
你是资深教育专家，精通以下领域：
1. K-12 各学科知识（数学/物理/化学/生物/历史/语文）
2. 教学设计：Bloom 认知分类、建构主义学习、掌握学习法
3. PPT 设计原则：每页一个核心信息、文字密度 ≤20%、必须有视觉元素
4. 理科要求：准确性优先，公式必须完整带单位，不能跳步

【角色约束】
- 科目类型：{subjectType}（science | humanities）
- 教学对象：{audience}（小学生/初中生/高中生/大学生/成人）
- 输出语言：{language}（中文 | 英文）

【任务】
为章节「{chapterTitle}」设计 {slideCount} 张幻灯片。

【叙事弧约束 - 理科必须执行四步法】
Step 1：问题引入（problem 类型，必须有一个生活中可操作的问题）
Step 2：概念讲解（concept/derivation 类型，公式必须完整）
Step 3：例题演练（exercise 类型，例题必须有完整解答）
Step 4：总结提升（summary/exit-ticket 类型）

【互动设计约束 - 每章至少 3 个互动节点】
在叙事弧的 3 个关键节点各插入 1 个互动：
- 导入环节 → vote 或 poll
- 概念讲解后 → quick-fire 或 think-pair-share
- 练习结束后 → exit-ticket 或 game

【幻灯片内容要求】
- slideType 必须是以下之一：
  text|diagram|infographic|comic|case|quote|quiz|activity|reflection
  |problem|concept|exercise|derivation
  |vote|poll|quick-fire|experiment|game|exit-ticket|result-viz
- 每张幻灯片的 mainBody 不超过 100 字
- 理科 slides 的 formula 字段必须包含完整数学表达式
- 所有数据必须标注来源

【输出格式】
直接输出 JSON，不要有任何前缀或解释：
{
  "chapterTitle": "...",
  "narrativeArc": { "opening": "...", "exploration": "...", "discovery": "...", "application": "..." },
  "learningObjectives": ["...", "..."],
  "interactionNodes": [{ "type": "...", "question": "...", "triggerMoment": "..." }],
  "slides": [
    {
      "slideType": "...",
      "title": "...",
      "mainBody": "...",
      "bullets": ["...", "..."],
      "formula": "...",
      "duration": 3,
      "teacherNote": "...",
      "visualGuidance": "...",
      "layout": "...",
      "artStyle": "..."
    }
  ],
  "totalDuration": 20
}
```

### 6.5 主流 AI PPT 生成工具调研

| 工具 | 定位 | AI 能力 | 模板质量 | 适合场景 |
|------|------|---------|---------|---------|
| Gamma | AI-first PPT | 生成完整 PPT（文字+图） | ⭐⭐⭐⭐⭐ | 快速生成演示文稿 |
| Beautiful.ai | 智能排版 | 模板智能适配 | ⭐⭐⭐⭐ | 不会设计的人 |
| Tome | 叙事型 PPT | 文字驱动，AI 生成 | ⭐⭐⭐⭐ | 讲故事/提案 |
| Slidebean | 商业计划 | AI 内容填充 | ⭐⭐⭐ | 商业场景 |
| Canva AI | 设计工具 | 文字→设计 | ⭐⭐⭐⭐ | 设计素材 |
| 闪击 PPT | 中文 AI PPT | 中文 Prompt 输入 | ⭐⭐⭐⭐ | 中国用户 |
| 博思白板 | 白板工具 | 文字→PPT | ⭐⭐⭐⭐ | 互动白板 |

**博思白板 / 闪击 PPT 的优势（值得借鉴）**：
- 对中文教育场景有专项优化（教材同步、知识点标签）
- 模板库中有大量"互动课件"模板（投票/讨论/实验）
- 支持导出 PPTX，可与本模块互补

### 6.6 推荐技术方案总结

| 阶段 | 内容生成 | 图片生成 | 说明 |
|------|---------|---------|------|
| Phase 1（最小可行） | GPT-4o-mini | DALL-E 3 | 最快出效果 |
| Phase 2（中文优化） | GLM-4 + GPT-4o-mini | 通义万相 + DALL-E 3 | 中文内容用国产 |
| Phase 3（成本优化） | GPT-4o-mini + Gemini 2.0 Flash | Gemini 2.0 Flash | 成本降至 $0.35/千章节 |
| Phase 4（多模态融合） | GPT-4o（主）+ Gemini 2.0 Flash（辅） | Gemini 2.0 Flash（原生图） | 最佳质量 |

---


## 7. 现有 Skill 整合策略

### 7.1 baoyu-infographic — 图表/信息图生成

**作用**：为每个知识点生成配套信息图，代替大段文字说明。

**整合方式**：
- `AIEnrichmentService` 在生成 `SlideContent` 时，根据知识点的结构选择合适的 `layout`（来自 baoyu-infographic 的 21 种布局）：
  - 概念对比 → `binary-comparison`
  - 流程/步骤 → `linear-progression`
  - 层级关系 → `hierarchical-layers`
  - 中心发散 → `hub-spoke`
  - 循环/周期 → `circular-flow`
  - 高密度总结 → `dense-modules`

- 生成图片 URL → 存入 `SlideContent.assetUrl` → 传给 `generate-ppt.js` 作为图片占位符

**推荐组合**（教学场景）：

| 内容 | 布局 | 风格 |
|------|------|------|
| 知识点总结 | `bento-grid` | `hand-drawn-edu` |
| 历史/时间线 | `linear-progression` | `vintage` |
| 概念对比 | `binary-comparison` | `corporate-memphis` |
| 分类/层级 | `periodic-table` | `bold-graphic` |
| 步骤流程 | `linear-progression` | `ikea-manual` |
| 循环规律 | `circular-flow` | `chalkboard` |
| 密集信息 | `dense-modules` | `pop-laboratory` |

### 7.2 baoyu-comic — 知识漫画生成

**作用**：将抽象概念转化为 2-4 格漫画，提升趣味性。

**整合方式**：
- `AIEnrichmentService` 为每个章节生成 1-2 个 `slideType: "comic"` 的幻灯片
- `comicPanels` 字段描述漫画分镜（scene + dialogue + caption）
- 漫画图片通过 `image_generate` 工具生成 → 上传 MinIO → URL 存入 `SlideContent.assetUrl`

**适用场景**：
- 概念引入（用生活场景解释抽象概念）
- 原理揭示（用比喻/拟人化角色讲解原理）
- 易错点澄清（用错误 vs 正确的对比漫画）

### 7.3 powerpoint — PPT 设计与渲染

**作用**：基于 `powerpoint` skill 的设计规范，指导 JS 层幻灯片渲染。

**关键设计原则**（来自 powerpoint skill）**应用到教学 PPT**：

| powerpoint skill 原则 | 在教学 PPT 中的应用 |
|----------------------|-------------------|
| 每张幻灯片需要一个视觉元素 | 每个知识点配一个信息图/漫画/图标 |
| 选择内容适配的配色方案 | 根据学科（理科/文科/艺术）选择模板 |
| 字体配对：标题有个性，正文干净 | 标题用粗体有冲击力，正文用无衬线清晰可读 |
| 布局多样化 | 交替使用：全图背景 / 双栏 / 卡片网格 / 居中大字 |
| 避免 AI 生成痕迹（不用标题下划线） | 使用色块/留白/图标替代下划线装饰 |
| 大号数字统计展示 | 用于"你知道吗"等趣味数据展示 |

---

## 8. PPT 构建流程（重构）

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
         │    │
         │    ├─ slideType == "comic" || "infographic"
         │    │    → 下载图片 → 上传 MinIO → assetUrl
         │    │    → JS 层使用图片占位符渲染
         │    │
         │    └─ slideType == 其他
         │         → 根据 slideType 映射到 JS 渲染函数
         │         → visualGuidance + artStyle 影响布局/配色
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

## 9. 幻灯片类型 → JS 渲染函数映射

| slideType | JS 函数 | 布局特点 | 视觉来源 |
|-----------|---------|---------|---------|
| problem | `createProblemSlide()` | 大字问题居中 + 生活场景图 | PptxGenJS |
| concept | `createConceptSlide()` | 公式突出 + 概念图 + 要点 | PptxGenJS |
| derivation | `createDerivationSlide()` | 分步推导，每步一行，逻辑清晰 | PptxGenJS |
| exercise | `createExerciseSlide()` | 例题→解答→变式题三段式 | PptxGenJS |
| summary | `createSummarySlide()` | 一句话+知识结构图+思考题 | PptxGenJS |
| infographic | `createInfographicSlide()` | 知识图谱/卡片网格/中心发散 | baoyu-infographic |
| comic | `createComicSlide()` | 2-4格漫画 + 旁白 | baoyu-comic |
| diagram | `createDiagramSlide()` | 流程图/对比/时间线 | PptxGenJS |
| case | `createCaseSlide()` | 案例标题 + 背景 + 分析要点 | 文字+图示 |
| quote | `createQuoteSlide()` | 大字名言居中 + 作者 + 解读 | 装饰文字 |
| quiz | `createQuizSlide()` | 题目 + 选项列表 | PptxGenJS |
| reflection | `createReflectionSlide()` | 问题突出显示 + 引导文字 | PptxGenJS |
| timeline | `createTimelineSlide()` | 时间轴 + 里程碑 | PptxGenJS |
| activity | `createActivitySlide()` | 活动标题 + 步骤列表 | PptxGenJS |
| text | `createTextSlide()` | 标题 + 正文 + 要点（控制行数） | PptxGenJS |
| title | `createTitleSlide()` | （已有）封面 | PptxGenJS |
| chapter | `createChapterSlide()` | （已有）章节封面 | PptxGenJS |
| end | `createEndSlide()` | （已有）结束页 | PptxGenJS |

---

## 10. API 设计

```
POST /api/material/ppt/generate
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "courseId": 1,               // 必填：关联课程
  "subjectType": "science",   // 新增：science（理科）| humanities（文科）
  "template": "elegant",       // 可选：default | elegant | minimal | vibrant | academic | hand-drawn-edu | chalkboard | kawaii | corporate-memphis
  "style": "academic",         // 可选：academic(学术) | vivid(生动) | concise(精炼)
  "includeQuiz": true,          // 可选：是否包含测验页（默认 true）
  "includeActivity": true,     // 可选：是否包含活动页（默认 true）
  "includeComic": true,         // 新增：是否包含知识漫画（默认 true）
  "includeInfographic": true,  // 新增：是否包含信息图（默认 true）
  "includeInteraction": true,   // 新增：是否包含互动节点（默认 true）
  "interactionDensity": "normal", // 新增：互动密度 low|normal|high
  "slidesPerChapter": 5,       // 可选：每章幻灯片数量（默认 4-6 自动）
  "artStyle": "hand-drawn-edu" // 新增：美术风格（默认 hand-drawn-edu）
}

Response:
{
  "code": 200,
  "data": {
    "materialId": 123,
    "fileUrl": "https://minio/.../xxx.pptx",
    "totalSlides": 28,
    "totalDuration": 45,
    "assetUrls": [              // 新增：生成的图片/漫画 URL
      "https://minio/.../infographic-ch1.png",
      "https://minio/.../comic-ch1.png"
    ],
    "chapters": [
      {
        "chapterId": 1,
        "title": "电磁感应基础",
        "slideCount": 6,
        "slides": [
          { "type": "infographic", "title": "...", "assetUrl": "..." },
          { "type": "comic", "title": "...", "assetUrl": "..." },
          { "type": "quiz", "title": "...", "questionCount": 2 }
        ]
      }
    ]
  }
}
```

---

## 11. 模板系统扩展

在现有 4 套模板基础上，结合 powerpoint skill 的配色规范，扩展为 9 套模板：

```javascript
const TEMPLATES = {
  default:  { name: '学院蓝',   colors: {...}, fontTitle: 'Microsoft YaHei', fontBody: 'SimSun',
              suitable: '通用场景' },
  elegant:  { name: '典雅绿',   colors: {...}, fontTitle: 'SimHei', fontBody: 'Microsoft YaHei',
              suitable: '自然科学' },
  minimal:  { name: '简约白',   colors: {...}, fontTitle: 'Helvetica Neue', fontBody: 'Arial',
              suitable: '简洁演示' },
  vibrant:  { name: '活力橙',   colors: {...}, fontTitle: 'PingFang SC', fontBody: 'Microsoft YaHei',
              suitable: '小学/初中' },
  academic: { name: '学术风',   colors: { primary: '1A3A5C', secondary: '2E6B8A', accent: '5BA4C4',
              fontTitle: 'Georgia', fontBody: 'Times New Roman', suitable: '大学/研究生' },
  hand-drawn-edu: { name: '手绘教育风', colors: { primary: '5D4E37', secondary: '8B7355', accent: 'F4A460',
              fontTitle: 'KaiTi', fontBody: 'KaiTi', suitable: '小学/趣味课堂',
              background: 'cream', texture: 'paper' },   // 模拟手写纸张质感
  chalkboard: { name: '黑板风', colors: { primary: '1A1A1A', secondary: '2D4A2D', accent: '7CB342',
              fontTitle: 'Ma Shan Zheng', fontBody: 'KaiTi', suitable: '互动课堂',
              background: 'dark-green', textColor: 'white' },  // 黑板背景
  kawaii: { name: '可爱风', colors: { primary: 'FF9ECD', secondary: 'FFE4EC', accent: 'FF6B9D',
              fontTitle: 'PingFang SC', fontBody: 'PingFang SC', suitable: '小学低年级',
              border: 'rounded', icons: 'cute' },  // 圆角卡片、可爱图标
  corporate-memphis: { name: '孟菲斯风', colors: { primary: '3B82F6', secondary: 'F472B6', accent: 'FBBF24',
              fontTitle: 'Arial Black', fontBody: 'Arial', suitable: '培训/企业',
              shapes: 'geometric' },  // 几何图形装饰、扁平插画风
}
```

**模板选择策略**（根据 audience + 学科自动推荐）：

| 受众 | 学科 | 推荐模板 |
|------|------|---------|
| 小学生 | 任意 | `kawaii` + `hand-drawn-edu` |
| 初中生 | 理科（数学/物理/化学） | `hand-drawn-edu` + `academic` |
| 初中生 | 文科 | `vibrant` + `hand-drawn-edu` |
| 高中生 | 理科 | `academic` + `elegant` |
| 高中生 | 文科 | `elegant` + `minimal` |
| 大学生 | 理科 | `academic` + `minimal` |
| 大学生 | 文科 | `minimal` + `elegant` |
| 成人/培训 | 任意 | `corporate-memphis` + `default` |
| 互动课堂 | 任意 | `chalkboard` + 互动组件（投票条/计时器/计分板） |
| 互动课堂 | 文科 | `chalkboard` |

---

## 12. 视觉资产生成流程

### 9.1 图片/信息图生成（baoyu-infographic 整合）

```
AIEnrichmentService 生成 SlideContent
    │
    ▼
检测 slideType == "infographic"
    │
    ├─ 读取 visualGuidance → layout（如 "hub-spoke"）
    ├─ 读取 artStyle → style（如 "hand-drawn-edu"）
    ├─ 组装 baoyu-infographic prompt
    └─ 调用 image_generate(prompt, aspect="landscape")
            │
            ▼
    下载图片 → 上传 MinIO → assetUrl
            │
            ▼
    SlideContent.assetUrl = assetUrl
            │
            ▼
    传给 generate-ppt.js → 在对应位置插入图片
```

### 9.2 知识漫画生成（baoyu-comic 整合）

```
AIEnrichmentService 生成 SlideContent
    │
    ▼
检测 slideType == "comic"
    │
    ├─ 读取 comicPanels[] → 分镜描述
    ├─ 选择 art style: "ligne-claire"（线稿清晰，适合教育）
    ├─ 读取 artStyle → art（如 "ligne-claire"）
    ├─ 组装 baoyu-comic prompt（含角色定义、场景描述）
    └─ 调用 image_generate(prompt, aspect="landscape")
            │
            ▼
    下载图片 → 上传 MinIO → assetUrl
            │
            ▼
    SlideContent.assetUrl = assetUrl
            │
            ▼
    createComicSlide(assetUrl) → 渲染到 PPT
```

---

## 13. 异步化改造（可选）

当前 PPT 生成是同步的，文件较大时可能超时：

```
POST /api/material/ppt/generate
  → 返回 taskId（202 Accepted）
  → 前端轮询 GET /api/material/task/{taskId}

AsyncTask 表状态：PENDING → PROCESSING → COMPLETED / FAILED
```

**异步任务分阶段**：
1. `PENDING` — 任务创建
2. `OUTLINE_ENRICHING` — AI 充实课程大纲
3. `INFOGRAPHIC_GENERATING` — 生成信息图/漫画（可并行）
4. `PPT_BUILDING` — 组装 PPT 文件
5. `UPLOADING` — 上传 MinIO
6. `COMPLETED` / `FAILED`

---

## 14. 实现计划

| 阶段 | 任务 | 优先级 | 依赖 |
|------|------|--------|------|
| Phase 1a | JS 层：新增 7 种幻灯片渲染函数（diagram/timeline/comic/infographic/quote/activity/reflection） | P0 | — |
| Phase 1b | JS 层：整合图片占位符渲染 + assetUrl 支持 | P0 | Phase 1a |
| Phase 2 | 新增 AIEnrichmentService + Mock 实现（提示词含趣味性策略） | P0 | — |
| Phase 3 | 重构 PptBuildService，集成 AIEnrichmentService | P0 | Phase 2 |
| Phase 4 | 整合 baoyu-infographic：图片生成 → MinIO → PPT | P1 | Phase 3 |
| Phase 5 | 整合 baoyu-comic：漫画生成 → MinIO → PPT | P1 | Phase 4 |
| Phase 6 | 扩展模板系统（9 套模板 + 自动推荐） | P1 | — |
| Phase 7 | API 参数扩展（includeComic/includeInfographic/artStyle） | P1 | Phase 2 |
| Phase 8 | 异步化改造（AsyncTask + 轮询） | P2 | Phase 3 |
| Phase 9 | 模板选择算法（根据 audience + 学科自动推荐） | P2 | Phase 6 |

---

## 15. 测试验证

| 级别 | 内容 |
|------|------|
| 单元测试 | `AIEnrichmentService` — mock LLM provider，验证 JSON 解析 + 文字密度控制（≤3行） |
| 单元测试 | `PptBuildService` — 验证 ChapterContent → PptGenerateRequest 映射 + assetUrl 传递 |
| 集成测试 | 调用 `/api/material/ppt/generate`，验证 PPTX 文件可打开、内容非空 |
| 集成测试 | 验证 infographic/comic 图片 URL 有效且在 PPT 中正确渲染 |
| 视觉 QA | 生成 PPT → 转 PDF → 转图片 → 检查幻灯片布局、文字密度、视觉层次 |
| E2E | 前端选择"趣味模式" → 生成 PPT → 下载 → 人工审核趣味性 |

---

## 16. 自定义 Skill 建议：teaching-ppt

建议在 `.hermes/skills/` 下新建 `teaching-ppt` skill，沉淀教学 PPT 生成的方法论：

```
teaching-ppt/
├── SKILL.md                    # Skill 主文件
└── references/
    ├── slide-type-taxonomy.md  # 教学幻灯片类型分类学
    ├── narrative-design.md     # 叙事弧线设计指南
    ├── visual-strategy.md     # 文字→视觉转换策略
    ├── template-selection.md   # 模板选择算法
    └── prompts/
        ├── enrichment-prompt.md      # AIEnrichmentService 提示词模板
        ├── infographic-prompt.md     # baoyu-infographic 整合提示词
        └── comic-prompt.md           # baoyu-comic 整合提示词
```

**核心内容**：
1. **Slide Type Taxonomy**：15+ 种教学幻灯片类型的定义、适用场景、设计要点
2. **Narrative Design**：4 步叙事弧（开场→探索→发现→应用）的提示词模板
3. **Visual Strategy**："减少文字，增加视觉"的具体执行方案（何时用信息图/漫画/流程图/对比图）
4. **Template Selection Algorithm**：受众+学科 → 推荐模板的决策树

---

## 附录 A：现有相关 Skills

| Skill | 用途 | 在本模块中的角色 |
|-------|------|----------------|
| `powerpoint` | PPTxGenJS 创建/编辑规范 | JS 层设计规范（配色/字体/布局/QA） |
| `baoyu-infographic` | 信息图生成（21布局×21风格） | 为知识点生成配套信息图，代替文字 |
| `baoyu-comic` | 知识漫画生成（多风格） | 生成概念理解漫画，提升趣味性 |
