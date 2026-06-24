# 测验模块设计

## 1. 模块定位

测验模块根据课程内容生成随堂练习和测试题目，用于检验学生学习效果。

```
Course（课程）
    │
    ├── title：课程标题
    ├── script：讲稿脚本
    └── outline.chapters[].keyPoints：章节重点
           │
           ▼
QuizGenerationService.generateQuiz()
           │
           ▼
Quiz JSON（随堂测验题目）
    │
    ├── 推送给学生端（答题）
    ├── 结果聚合（统计正确率）
    └── 存库（可选，供复习使用）
```

---

## 2. 现状分析

### 2.1 现有 QuizGenerationService

```java
public QuizGenerateResponse generateQuiz(QuizGenerateRequest request, Long userId) {
    // 1. 根据 courseId 获取 Course
    // 2. 构建 Prompt（Course.title + Course.script）
    // 3. 调用 AIService.chat() 获取 LLM 生成的题目
    // 4. 解析 JSON 响应为 QuizGenerateResponse
}
```

### 2.2 现有 QuizGenerateRequest

```java
public class QuizGenerateRequest {
    Long courseId;
    String subject;
    String grade;
    String difficulty;   // easy / medium / hard
    Integer count;       // 题目数量
    String type;         // choice / blank / essay / mixed
}
```

### 2.3 现有 QuizGenerateResponse

```java
public class QuizGenerateResponse {
    Long courseId;
    List<Question> questions;

    public static class Question {
        String type;          // choice / blank / essay
        String content;       // 题目内容
        List<String> options; // 选项（A/B/C/D）
        String answer;        // 答案
        String explanation;   // 解析
    }
}
```

### 2.4 现有问题

| 问题 | 说明 |
|------|------|
| 只用 title + script | 没有利用章节结构（keyPoints）和幻灯片内容 |
| 题型单一 | 只有 choice/blank/essay，缺少操作题/实验题 |
| 没有区分文理科 | 理科缺少计算题/证明题 |
| 没有和 PPT 互动节点联动 | PPT 中设计的 quiz slide 没有被复用 |
| 没有持久化 | 每次生成都重新调用 LLM，无法复用 |
| 没有难度梯度设计 | 题目没有从易到难的梯度分布 |

---

## 3. 目标

| 目标 | 说明 |
|------|------|
| 基于 Course outline 生成 | 利用章节结构 + 幻灯片内容生成更精准的题目 |
| 题型丰富 | 选择/填空/简答/计算/证明/实验/案例分析 |
| 文理科差异化 | 理科：计算+证明；文科：分析+表达 |
| 难度梯度 | 每套题按 easy → medium → hard 分布 |
| 互动联动 | 复用 PPT 中的 quiz slide 作为种子题目 |
| 持久化 | 保存 Quiz 到数据库，支持重复使用 |
| 答案解析 | 每题必须包含详细解析 |

---

## 4. 数据模型

### 4.1 测验请求（扩展）

```java
public class QuizGenerateRequest {
    Long courseId;              // 课程ID（必填）

    // 题目配置
    Integer totalCount;         // 总题数（默认10）
    String difficulty;         // 难度：easy/medium/hard/mixed（默认mixed）
    List<String> questionTypes; // 题型列表（默认["choice"]）
    // 可选：choice / blank / essay / calculation / proof / experiment / case

    // 生成控制
    Boolean includeExplanation; // 是否包含解析（默认true）
    Boolean includeAnswer;     // 是否包含答案（默认true，出题模式用）
    String language;           // 题目语言（默认中文）

    // 知识点覆盖
    List<Long> chapterIndexes;  // 指定生成哪些章节的题目（默认全部）
    List<String> keyPoints;    // 指定覆盖哪些知识点（可选）

    // 特殊需求
    Boolean reusePptQuizzes;    // 是否复用PPT中已有的quiz slide（默认true）
}
```

### 4.2 测验响应

```java
public class QuizGenerateResponse {
    Long courseId;
    Long quizId;                  // 持久化后的ID（可选）
    String title;                 // 测验标题
    Integer totalDuration;        // 建议时长（分钟）
    List<QuizQuestion> questions;

    public static class QuizQuestion {
        Integer index;            // 题号
        String type;              // 题型
        String content;           // 题目内容
        List<String> options;     // 选项（A/B/C/D/E）
        String answer;            // 答案
        String explanation;       // 详细解析
        String difficulty;         // easy / medium / hard
        Integer estimatedTime;    // 预估答题时间（秒）
        List<String> relatedKeyPoints; // 关联的知识点
        String chapterTitle;      // 所属章节标题
    }
}
```

### 4.3 持久化模型（Quiz / QuizQuestion）

```sql
CREATE TABLE quiz (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id       BIGINT NOT NULL,
    title           VARCHAR(255),
    total_count     INT,
    difficulty      VARCHAR(16),
    total_duration  INT              COMMENT '建议时长（分钟）',
    status          VARCHAR(32)      COMMENT 'draft/published/archived',
    creator_id      BIGINT,
    created_at      DATETIME,
    deleted         BOOLEAN DEFAULT FALSE,
    INDEX idx_course (course_id)
);

CREATE TABLE quiz_question (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    quiz_id         BIGINT NOT NULL,
    index_num       INT,
    question_type   VARCHAR(32),
    content         TEXT,
    options         JSON,
    answer          TEXT,
    explanation     TEXT,
    difficulty      VARCHAR(16),
    related_kp      VARCHAR(255)     COMMENT '关联知识点，逗号分隔',
    chapter_index   INT,
    created_at      DATETIME,
    deleted         BOOLEAN DEFAULT FALSE,
    INDEX idx_quiz (quiz_id)
);
```

---

## 5. 文理科差异化题型

| 学科类型 | 推荐题型 | 说明 |
|---------|---------|------|
| 理科（science） | choice / blank / calculation / proof / experiment | 数学→计算/证明；物理→实验；化学→计算/实验 |
| 文科（humanities） | choice / blank / essay / case | 历史→分析/案例；语文→阅读/写作 |

**理科计算题模板**：

```json
{
  "type": "calculation",
  "content": "已知 a=3, b=4，求 c，使得 a² + b² = c²",
  "answer": "c=5",
  "explanation": "由勾股定理...",
  "formula": "a² + b² = c²",
  "solutionSteps": [
    { "step": "代入已知条件", "result": "3² + 4² = c²" },
    { "step": "计算平方", "result": "9 + 16 = c²" },
    { "step": "求和", "result": "25 = c²" },
    { "step": "开方", "result": "c = 5" }
  ]
}
```

---

## 6. LLM 提示词模板

```
【系统提示】
你是资深{subject}学科出题专家，精通K-12各学段考试命题规范。

【输入信息】
- 课程标题：{courseTitle}
- 章节重点：{keyPoints}
- 讲稿摘要：{scriptSummary}
- 题目数量：{totalCount}
- 难度分布：{difficultyDistribution}
  - easy：基础概念辨析（30%）
  - medium：应用分析（50%）
  - hard：综合拓展（20%）
- 题型要求：{questionTypes}

【理科（science）附加要求】
- 计算题必须有完整解题步骤（stepByStep）
- 证明题每步必须有依据
- 公式必须带单位
- 答案必须配有效数字

【题目设计原则】
1. 每道题只考察一个核心知识点
2. 选项要具有区分度（不要有明显错误选项）
3. 题目语言简洁，无歧义
4. 解析要指出解题关键步骤
5. 估算每道题答题时间（秒）

【输出格式】
直接输出JSON：
{
  "title": "测验标题",
  "totalDuration": 20,
  "questions": [
    {
      "index": 1,
      "type": "choice|blank|calculation|proof|experiment|essay|case",
      "content": "题目内容",
      "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
      "answer": "B",
      "explanation": "解析：...",
      "difficulty": "easy|medium|hard",
      "estimatedTime": 60,
      "relatedKeyPoints": ["相关知识点"],
      "chapterTitle": "章节标题"
    }
  ]
}
```

---

## 7. Service 层实现

```java
@Service
public class QuizGenerationService {

    @Autowired private CourseMapper courseMapper;
    @Autowired private AIService aiService;
    @Autowired private QuizMapper quizMapper;
    @Autowired private QuizQuestionMapper quizQuestionMapper;
    @Autowired private ObjectMapper objectMapper;

    /**
     * 生成测验
     * 1. 获取 Course 数据（title + script + outline.keyPoints）
     * 2. 提取 PPT 中已有的 quiz slide 作为种子题
     * 3. 调用 LLM 生成题目
     * 4. 持久化到数据库（可选）
     * 5. 返回 QuizGenerateResponse
     */
    public QuizGenerateResponse generateQuiz(QuizGenerateRequest request,
                                              Long userId);

    /**
     * 从 PPT 提取已有测验题（种子题）
     */
    public List<QuizQuestion> extractFromPptSlides(Course course);

    /**
     * 持久化测验到数据库
     */
    public Long saveQuiz(QuizGenerateResponse response, Long courseId, Long userId);

    /**
     * 获取测验详情
     */
    public QuizGenerateResponse getQuizDetail(Long quizId);

    /**
     * 批量获取测验列表
     */
    public PageResult<QuizVO> listQuizzes(Long courseId, PageRequest request);
}
```

---

## 8. 与 PPT 模块的联动

PPT 模块在生成幻灯片时，会在 `outline.chapters[].slides` 中插入 `slideType: "quiz"` 的幻灯片。测验模块可以复用这些已有的测验题作为种子题：

```java
// 提取 PPT 中已设计的测验题
public List<QuizQuestion> extractFromPptSlides(Course course) {
    CourseOutline outline = parseOutline(course.getOutline());
    List<QuizQuestion> seedQuestions = new ArrayList<>();

    for (Chapter chapter : outline.getChapters()) {
        for (SlideContent slide : chapter.getSlides()) {
            if ("quiz".equals(slide.getSlideType())) {
                // 将 PPT 中的 quiz slide 转换为 QuizQuestion
                seedQuestions.add(convertToQuizQuestion(slide, chapter.getTitle()));
            }
        }
    }
    return seedQuestions;
}
```

---

## 9. 实现计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 扩展 QuizGenerateRequest/Response，新增计算/证明/实验题型 | P0 |
| Phase 2 | 文理科差异化提示词（理科加重计算/证明题） | P0 |
| Phase 3 | 难度梯度分布（easy30% / medium50% / hard20%） | P0 |
| Phase 4 | 持久化到 Quiz/QuizQuestion 表 | P1 |
| Phase 5 | 复用 PPT 中已有的 quiz slide 作为种子题 | P1 |
| Phase 6 | 题目统计分析（正确率/难度评估/知识点覆盖） | P2 |
