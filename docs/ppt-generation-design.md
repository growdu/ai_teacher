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

---

## 2. 目标

PPT 内容经 AI 二次处理，每张幻灯片包含足够的教学文字、视觉层次和教学设计元素（案例、测验、讨论等）。

**新增核心目标**：

- **趣味性**：引入知识漫画、故事线、互动问答等趣味教学元素
- **生动性**：用图表、信息图、流程动画代替大段文字
- **格式多样化**：多种模板 + 多种教学幻灯片类型
- **减少文字密度**：每张幻灯片不超过 3 行文字，以视觉元素承载信息

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
    private String slideType;    // text | diagram | case | quote | quiz | activity | reflection | comic | infographic | timeline | problem | concept | exercise | derivation
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
    private String type;    // 新增: text|diagram|case|quote|quiz|activity|reflection|comic|infographic|timeline|problem|concept|exercise|derivation
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

## 6. 现有 Skill 整合策略

### 6.1 baoyu-infographic — 图表/信息图生成

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

### 6.2 baoyu-comic — 知识漫画生成

**作用**：将抽象概念转化为 2-4 格漫画，提升趣味性。

**整合方式**：
- `AIEnrichmentService` 为每个章节生成 1-2 个 `slideType: "comic"` 的幻灯片
- `comicPanels` 字段描述漫画分镜（scene + dialogue + caption）
- 漫画图片通过 `image_generate` 工具生成 → 上传 MinIO → URL 存入 `SlideContent.assetUrl`

**适用场景**：
- 概念引入（用生活场景解释抽象概念）
- 原理揭示（用比喻/拟人化角色讲解原理）
- 易错点澄清（用错误 vs 正确的对比漫画）

### 6.3 powerpoint — PPT 设计与渲染

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

## 7. PPT 构建流程（重构）

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

## 8. 幻灯片类型 → JS 渲染函数映射

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

## 9. API 设计

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

## 10. 模板系统扩展

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
| 互动课堂 | 理科（物理/化学实验） | `chalkboard`（模拟实验室黑板氛围） |
| 互动课堂 | 文科 | `chalkboard` |

---

## 11. 视觉资产生成流程

### 11.1 图片/信息图生成（baoyu-infographic 整合）

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

### 11.2 知识漫画生成（baoyu-comic 整合）

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

## 12. 异步化改造（可选）

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

## 13. 实现计划

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

## 14. 测试验证

| 级别 | 内容 |
|------|------|
| 单元测试 | `AIEnrichmentService` — mock LLM provider，验证 JSON 解析 + 文字密度控制（≤3行） |
| 单元测试 | `PptBuildService` — 验证 ChapterContent → PptGenerateRequest 映射 + assetUrl 传递 |
| 集成测试 | 调用 `/api/material/ppt/generate`，验证 PPTX 文件可打开、内容非空 |
| 集成测试 | 验证 infographic/comic 图片 URL 有效且在 PPT 中正确渲染 |
| 视觉 QA | 生成 PPT → 转 PDF → 转图片 → 检查幻灯片布局、文字密度、视觉层次 |
| E2E | 前端选择"趣味模式" → 生成 PPT → 下载 → 人工审核趣味性 |

---

## 15. 自定义 Skill 建议：teaching-ppt

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
