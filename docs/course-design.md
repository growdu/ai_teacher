# 课程管理模块设计

## 1. 模块定位

**课程（Course）** 是知识点的**第一次AI加工产物**，也是 PPT/视频/测验等二次生成模块的**唯一数据中转**。

```
知识点（KnowledgePoint）
    │
    │  CourseGenerateService.generateCourse()
    ▼
课程（Course）
    ├── outline（JSON）：PPT生成的核心输入
    ├── script（Text）：视频生成的核心输入
    ├── title/description：测验生成的输入
    └── metadata（subjectType/targetAudience/totalDuration）
```

**设计原则**：Course 必须是**自包含**的——给定一个 Course 对象，不需要额外查库就能驱动所有下游生成模块。

---

## 2. 现状分析

### 2.1 现有实体

```java
// 现有 Course.java 字段
@Entity
public class Course {
    Long id;
    Long tenantId;
    Long workspaceId;
    Long knowledgePointId;   // 关联知识点
    String title;            // 课程标题
    String outline;          // 课程大纲（JSON格式，chapters）
    String script;           // 讲稿脚本
    String status;           // 状态：draft/generated/published
    Long creatorId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean deleted;
}
```

### 2.2 现有 CourseOutline JSON 结构

```json
{
  "title": "课程标题",
  "description": "课程简介",
  "chapters": [
    {
      "title": "章节标题",
      "duration": 10,
      "keyPoints": ["重点1", "重点2"],
      "teachingNotes": "教学备注"
    }
  ],
  "totalDuration": 40
}
```

### 2.3 现有问题

| 问题 | 说明 |
|------|------|
| outline 过于简单 | 只有章节骨架，没有幻灯片设计、互动节点、叙事弧 |
| 缺少 subjectType | 无法区分理科/文科，PPT生成策略无法精准选择 |
| 缺少 targetAudience | 无法确定内容深度和语言风格 |
| 缺少 slides 数组 | PPT生成时 AIEnrichmentService 只能重新生成幻灯片内容 |
| script 生成质量不可控 | 没有分章节script，难以匹配具体幻灯片 |
| 缺少知识点的结构化输入 | CourseGenerateService 拿到的知识点信息太少 |

---

## 3. 目标

| 目标 | 说明 |
|------|------|
| outline 完整化 | 包含叙事弧、互动节点、幻灯片内容（slides数组） |
| 学科感知 | 自动从知识点继承 subjectType |
| 受众感知 | 自动从知识点 grade 推断 targetAudience |
| script 分章节 | 每章节对应独立 script 片段，可精准匹配PPT |
| 一次生成，多处复用 | Course 生成后立即触发 PPT/视频/测验并行生成 |

---

## 4. 数据模型

### 4.1 扩展后的 Course 实体

```java
@Entity
public class Course {
    Long id;
    Long tenantId;
    Long workspaceId;
    Long knowledgePointId;        // 关联知识点

    // 基本信息
    String title;                  // 课程标题
    String description;             // 课程简介

    // 学科与受众（从知识点继承，可覆盖）
    String subjectType;            // science / humanities（新增）
    String targetAudience;         // 小学生/初中生/高中生/大学生（新增）

    // 大纲（扩展后的完整JSON）
    String outline;                // 完整CourseOutline JSON（见4.2）

    // 讲稿（分章节，新增）
    String script;                 // 完整讲稿（总脚本）
    String scriptByChapter;        // 分章节讲稿JSON（新增）
    // scriptByChapter 示例：
    // [
    //   { "chapterIndex": 0, "title": "第一章", "script": "同学们好，今天我们..." },
    //   { "chapterIndex": 1, "title": "第二章", "script": "上一章我们学习了..." }
    // ]

    // 教学设计（新增）
    String teachingDesign;         // JSON，教学设计（教学目标/重难点/教学方法）
    // {
    //   "learningObjectives": ["理解XX概念", "掌握XX方法"],
    //   "teaching Schwerpunkt": ["重点：XX", "难点：XX"],
    //   "teachingMethods": ["讲授法", "讨论法", "实验法"]
    // }

    // 元数据
    String status;                  // draft/generating/generated/published
    String subject;                 // 学科：数学/物理/语文（新增）
    String grade;                   // 年级（新增）
    Integer totalDuration;          // 总时长（分钟，新增）
    Integer chapterCount;           // 章节数（新增）

    Long creatorId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean deleted;
}
```

### 4.2 完整 CourseOutline JSON 结构

```json
{
  "title": "二次函数及其图像",
  "description": "学习二次函数的概念、图像变换和实际应用",
  "subjectType": "science",
  "targetAudience": "初中生",
  "totalDuration": 45,
  "narrativeArc": {
    "opening": "用一个足球射门的问题引入：球的最高点在哪里？",
    "exploration": "通过描点法绘制不同a值的二次函数图像",
    "discovery": "揭示a、b、c三个参数对图像的直接影响",
    "application": "用二次函数求解实际最大/最小值问题"
  },
  "chapters": [
    {
      "title": "1.1 二次函数的定义",
      "duration": 10,
      "keyPoints": ["二次函数的定义", "二次项系数a≠0"],
      "teachingNotes": "重点讲清为什么a≠0",
      "slides": [
        {
          "slideType": "problem",
          "title": "生活中的抛物线",
          "mainBody": "一个篮球被投出后，它的轨迹是什么样的？",
          "bullets": ["抛物线形状", "最高点/最低点"],
          "duration": 2,
          "teacherNote": "让学生观察并回答",
          "visualGuidance": "篮球投篮轨迹图"
        },
        {
          "slideType": "concept",
          "title": "二次函数的定义",
          "mainBody": "形如 y = ax² + bx + c（a≠0）的函数叫二次函数",
          "bullets": ["a叫做二次项系数", "b叫做一次项系数", "c叫做常数项"],
          "formula": "y = ax² + bx + c（a ≠ 0）",
          "duration": 3,
          "teacherNote": "强调a≠0的重要性",
          "visualGuidance": "定义框+几何画板动画"
        }
      ],
      "interactionNodes": [
        {
          "type": "vote",
          "question": "以下哪个是二次函数？A. y=x² B. y=2x+1 C. y=1/x²",
          "triggerMoment": "概念讲解后",
          "expectedDuration": 2
        }
      ]
    },
    {
      "title": "1.2 二次函数的图像",
      "duration": 20,
      "keyPoints": ["开口方向", "对称轴", "顶点坐标"],
      "teachingNotes": "结合几何画板演示a、b、c对图像的影响",
      "slides": [],
      "interactionNodes": []
    }
  ],
  "learningObjectives": [
    "理解二次函数的定义",
    "掌握二次函数图像的性质",
    "能用二次函数解决实际问题"
  ],
  "teachingDesign": {
    "schwerpunkt": { "重点": "二次函数图像的性质", "难点": "参数a、b、c对图像的影响" },
    "methods": ["讲授法", "演示法", "讨论法"]
  }
}
```

---

## 5. API 设计

### 5.1 课程管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/course/generate` | 从知识点生成课程 |
| GET | `/api/course/{id}` | 获取课程详情（含outline） |
| PUT | `/api/course/{id}` | 更新课程（触发重新生成） |
| DELETE | `/api/course/{id}` | 删除课程 |
| GET | `/api/course/list` | 分页查询课程 |
| POST | `/api/course/{id}/publish` | 发布课程 |

### 5.2 课程生成请求

```json
POST /api/course/generate
{
  "knowledgePointId": 1,
  "subjectType": "science",         // 可选，自动从知识点继承
  "targetAudience": "初中生",         // 可选，自动推断
  "chapterCount": 3,                 // 可选，默认3章
  "includeScript": true,             // 是否生成讲稿
  "generateAfter": ["ppt", "video"]  // 生成后自动触发哪些：ppt/video/quiz
}
```

### 5.3 课程生成响应

```json
{
  "code": 200,
  "data": {
    "courseId": 1,
    "status": "generating",
    "estimatedDuration": 45,
    "followUp": {
      "pptMaterialId": null,        // PPT生成任务ID
      "videoMaterialId": null,       // 视频生成任务ID
      "quizTaskId": null              // 测验生成任务ID
    }
  }
}
```

---

## 6. Service 层实现

### 6.1 CourseGenerateService 核心方法

```java
@Service
public class CourseGenerateService {

    @Autowired private KnowledgePointMapper knowledgePointMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private AIService aiService;
    @Autowired private AsyncTaskService asyncTaskService;

    /**
     * 主流程：从知识点生成完整课程
     * 1. 获取知识点输入（KnowledgePointInput）
     * 2. 调用 LLM 生成 CourseOutline（chapters + slides）
     * 3. 调用 LLM 生成分章节讲稿（scriptByChapter）
     * 4. 保存 Course 记录
     * 5.（可选）并行触发 PPT/视频/测验生成
     */
    public Course generateCourse(CourseGenerateRequest request, Long userId);

    /**
     * 生成课程大纲（第一步）
     * 调用 LLM，传入知识点结构化内容，生成完整 CourseOutline
     */
    public CourseOutline generateOutline(CourseGenerateRequest request,
                                          KnowledgePointInput kpInput);

    /**
     * 生成分章节讲稿（第二步）
     * 基于 outline 每章节内容，生成独立 script 片段
     */
    public String generateScriptByChapter(CourseOutline outline);

    /**
     * 更新课程大纲（局部更新某章节的 slides）
     */
    public void updateChapterSlides(Long courseId, int chapterIndex,
                                     List<SlideContent> newSlides);

    /**
     * 课程生成后，触发下游模块
     */
    public void triggerDownstreamGenerations(Course course,
                                              List<String> generationTypes);
}
```

### 6.2 LLM 提示词模板

```
【系统提示】
你是资深教育专家，精通{subject}学科教学和课程设计。

【输入信息】
- 学科：{subject}
- 年级：{grade}
- 知识点内容：{knowledgePointContent}
- 结构化要点：{keyPoints}
- 公式（理科）：{formulas}
- 前置知识点：{prerequisiteTitles}
- 预估时长：{estimatedMinutes}分钟

【任务】
为上述知识点设计一门完整的课程。

【课程结构要求】
- 章节数：{chapterCount}章
- 每章必须包含：problem→concept→exercise→summary 四步法（理科）
- 每章必须包含：infographic/comic/case 至少1种视觉型幻灯片
- 每章必须包含：至少3个互动节点（vote/quick-fire/exit-ticket等）
- 叙事弧：opening→exploration→discovery→application

【输出格式】
直接输出JSON，格式如下（不得包含任何解释文字）：
{
  "title": "课程标题",
  "description": "课程简介",
  "subjectType": "science|humanities",
  "targetAudience": "小学生|初中生|高中生|大学生",
  "totalDuration": 45,
  "narrativeArc": { "opening": "...", "exploration": "...", "discovery": "...", "application": "..." },
  "chapters": [
    {
      "title": "章节标题",
      "duration": 10,
      "keyPoints": ["重点1", "重点2"],
      "teachingNotes": "教学备注",
      "slides": [
        {
          "slideType": "problem|concept|exercise|infographic|comic|quiz|vote|...",
          "title": "幻灯片标题",
          "mainBody": "主要内容（≤100字）",
          "bullets": ["要点1", "要点2"],
          "formula": "公式（理科必填）",
          "duration": 3,
          "teacherNote": "教师备注",
          "visualGuidance": "图片描述/布局建议"
        }
      ],
      "interactionNodes": [
        { "type": "vote|poll|quick-fire|exit-ticket", "question": "问题", "triggerMoment": "...", "expectedDuration": 2 }
      ]
    }
  ],
  "learningObjectives": ["目标1", "目标2", "目标3"]
}
```

---

## 7. 课程状态机

```
draft
  │
  │ generateCourse()
  ▼
generating
  │  ✓ outline生成成功 + script生成成功
  ▼
generated
  │  user 修改 outline 或触发重新生成
  ▼
published
  │  user 取消发布
  ▼
archived
```

**状态约束**：
- `generating` 状态下禁止任何修改
- `published` 状态下修改需创建新版本
- 删除仅允许 `draft` 状态

---

## 8. 与其他模块的接口

| 依赖模块 | 接口 | 说明 |
|---------|------|------|
| KnowledgePointService | `getInputForCourse(kpId)` | 获取标准知识点输入 |
| PptGenerationService | `generatePpt(courseId)` | 消费 Course.outline |
| VideoGenerationService | `generateVideo(courseId)` | 消费 Course.script |
| QuizGenerationService | `generateQuiz(courseId)` | 消费 Course.title + Course.script |
| TeachingMaterialService | `saveMaterial(course, type, fileUrl)` | 保存生成的教学材料 |

---

## 9. 实现计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 扩展 Course 实体（新增字段：subjectType, targetAudience, scriptByChapter, teachingDesign等） | P0 |
| Phase 2 | 重构 generateOutline 提示词，输出完整 slides 数组 + 互动节点 | P0 |
| Phase 3 | 实现 scriptByChapter 分章节讲稿生成 | P1 |
| Phase 4 | 课程生成后并行触发 PPT/视频/测验（AsyncTask） | P1 |
| Phase 5 | 课程版本管理（published 状态下修改创建新版本） | P2 |
