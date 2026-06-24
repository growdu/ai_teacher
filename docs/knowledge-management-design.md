# 知识点管理模块设计

## 1. 模块定位

**知识点（KnowledgePoint）** 是整个平台的**内容原子**。所有教学资源（课程/PPT/视频/测验）都从知识点出发生成。

```
知识点（KnowledgePoint）
    │
    │  1:N
    ▼
课程（Course）
    │
    ├──▶ PPT（TeachingMaterial.type=ppt）
    ├──▶ 视频（TeachingMaterial.type=video）
    └──▶ 测验（Quiz JSON）
```

---

## 2. 现状分析

### 2.1 现有实体

```java
// 现有 KnowledgePoint.java 字段
@Entity
public class KnowledgePoint {
    Long id;
    Long tenantId;
    Long workspaceId;
    String subject;     // 学科：数学/物理/化学/语文/历史
    String grade;       // 年级：初一/高一/大一
    String content;     // 知识点正文
    String tags;        // 标签（逗号分隔）
    String difficulty;  // 难度：easy/medium/hard（缺失，需新增）
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean deleted;
}
```

### 2.2 现有问题

| 问题 | 说明 |
|------|------|
| content 是纯文本 | 没有结构化，无法区分"概念定义"和"例题" |
| 缺少 subjectType 字段 | 无法区分理科/文科，影响生成策略 |
| 缺少前置/后置依赖 | 无法构建知识图谱和学习路径 |
| 标签体系不完善 | 无标准知识点标签，影响课程组装 |
| 缺少关联资源 | 没有和已有PPT/视频/测验关联 |

---

## 3. 目标

| 目标 | 说明 |
|------|------|
| 结构化知识点 | 区分概念、例题、公式、原理等不同类型内容 |
| 学科分类 | 明确 subjectType（science/humanities） |
| 知识图谱 | 建立前置/后置依赖关系，支持学习路径规划 |
| 标准标签 | 对齐教材目录，支持精准检索 |
| 质量评估 | 知识点完整性评分，辅助教师优化 |

---

## 4. 数据模型

### 4.1 扩展后的 KnowledgePoint 实体

```java
@Entity
public class KnowledgePoint {
    Long id;
    Long tenantId;
    Long workspaceId;

    // 学科信息
    String subject;          // 学科：数学/物理/化学/生物/语文/历史/地理/政治
    String grade;            // 年级：初一/初二/初三/高一/高二/高三/大一/大二
    String subjectType;      // 学科类型：science（理科）/ humanities（文科）// 新增

    // 内容结构（JSON格式）
    String content;          // 知识点正文（纯文本，LLM输入）
    String contentStructured; // 结构化内容（JSON，新增）
    // contentStructured 示例：
    // {
    //   "type": "concept|formula|principle|rule|example",
    //   "coreDefinition": "核心定义",
    //   "keyPoints": ["要点1", "要点2"],
    //   "formulas": ["公式1", "公式2"],          // 理科
    //   "examples": ["例题1", "例题2"],
    //   "commonMistakes": ["易错点1", "易错点2"],
    //   "relatedConcepts": ["相关概念1", "相关概念2"]
    // }

    // 知识图谱
    String prerequisiteIds;   // 前置知识点ID列表（逗号分隔，新增）
    String relatedIds;         // 相关知识点ID列表（逗号分隔，新增）

    // 元数据
    String tags;              // 标签：教材章节标签/难度标签
    String difficulty;         // 难度：easy/medium/hard
    Integer estimatedMinutes; // 预估讲解时长（分钟，新增）

    // 关联统计
    Integer courseCount;      // 关联课程数量（生成后更新）
    Integer resourceCount;     // 关联资源数量（生成后更新）

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean deleted;
}
```

### 4.2 枚举定义

```java
public enum SubjectType {
    SCIENCE("science", "理科"),      // 数学/物理/化学/生物
    HUMANITIES("humanities", "文科"); // 语文/历史/地理/政治

    public final String value;
    public final String label;
}

public enum KnowledgeType {
    CONCEPT("concept", "概念"),
    FORMULA("formula", "公式/定理"),   // 理科
    PRINCIPLE("principle", "原理/规律"),
    RULE("rule", "规则/定律"),
    EXAMPLE("example", "例题"),
    EXPERIMENT("experiment", "实验"),   // 理化生
    PROOF("proof", "证明/推导");        // 数学
}
```

---

## 5. API 设计

### 5.1 知识点 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge/create` | 创建知识点 |
| GET | `/api/knowledge/{id}` | 获取知识点详情 |
| PUT | `/api/knowledge/{id}` | 更新知识点 |
| DELETE | `/api/knowledge/{id}` | 删除知识点（软删） |
| GET | `/api/knowledge/list` | 分页查询知识点 |

### 5.2 请求/响应模型

**创建知识点请求**：

```json
POST /api/knowledge/create
{
  "tenantId": 1,
  "workspaceId": 1,
  "subject": "数学",
  "grade": "高一",
  "subjectType": "science",
  "content": "三角函数的基本概念和诱导公式",
  "contentStructured": {
    "type": "concept",
    "coreDefinition": "三角函数是角度与比值的函数关系...",
    "keyPoints": ["正弦函数定义", "余弦函数定义", "正切函数定义"],
    "formulas": ["sin²α + cos²α = 1", "诱导公式：sin(90°-α)=cosα"],
    "examples": ["已知α=30°，求sinα的值"],
    "commonMistakes": ["混淆正弦和余弦的定义域"]
  },
  "prerequisiteIds": "1,3,5",
  "relatedIds": "2,4",
  "tags": "必修一,第三章,三角函数",
  "difficulty": "medium",
  "estimatedMinutes": 20
}
```

**知识点列表响应**：

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "subject": "数学",
        "grade": "高一",
        "subjectType": "science",
        "content": "三角函数...",
        "tags": "必修一,第三章",
        "difficulty": "medium",
        "courseCount": 2,
        "resourceCount": 5
      }
    ],
    "total": 100
  }
}
```

---

## 6. Service 层实现

### 6.1 KnowledgePointService 核心方法

```java
@Service
public class KnowledgePointService {

    @Autowired private KnowledgePointMapper knowledgePointMapper;

    /**
     * 创建知识点
     * 自动从 content 推断 subjectType（理科/文科）
     * 自动提取 tags（基于教材章节关键词）
     */
    public KnowledgePoint create(KnowledgePointCreateRequest request);

    /**
     * 更新知识点
     * 如果关联课程已生成，发出警告（课程需要重新生成）
     */
    public KnowledgePoint update(Long id, KnowledgePointUpdateRequest request);

    /**
     * 删除知识点（二次确认：有关联课程时禁止删除）
     */
    public void delete(Long id);

    /**
     * 检索知识点（按学科/年级/标签/难度过滤）
     */
    public PageResult<KnowledgePointVO> list(KnowledgePointQueryRequest request);

    /**
     * 获取知识点详情（含关联课程列表）
     */
    public KnowledgePointDetailVO getDetail(Long id);

    /**
     * 检索前置知识点（用于学习路径规划）
     */
    public List<KnowledgePoint> getPrerequisites(Long id);

    /**
     * 检索相关知识点
     */
    public List<KnowledgePoint> getRelated(Long id);

    /**
     * 批量创建（导入场景）
     */
    public List<KnowledgePoint> batchCreate(BatchKnowledgePointRequest request);
}
```

---

## 7. 知识点→课程生成接口

知识点模块为课程生成模块提供**标准输入格式**：

```java
/**
 * 为课程生成模块提供标准化的知识点输入
 */
public class KnowledgePointInput {
    Long knowledgePointId;
    String subjectType;           // science / humanities
    String subject;              // 数学/物理/化学/语文/历史
    String grade;                // 初一/高一/大一
    String content;              // 原始内容（LLM输入）
    String contentStructured;    // 结构化内容（JSON）
    List<String> keyPoints;      // 核心要点列表
    List<String> formulas;       // 公式列表（理科）
    List<String> prerequisiteTitles; // 前置知识点标题
    Integer estimatedMinutes;    // 预估时长
}
```

**调用链**：
```
用户选择知识点 → KnowledgePointService.getInputForCourse(kpId)
    → CourseGenerateService.generateCourse(kpInput)
    → 生成 Course（含 outline + script）
```

---

## 8. 标签体系

### 8.1 学科标签

| 学科 | 标签示例 |
|------|---------|
| 数学 | 代数/几何/三角/函数/概率/数列 |
| 物理 | 力学/热学/电磁学/光学/原子物理 |
| 化学 | 无机/有机/反应原理/元素周期/化学键 |
| 语文 | 散文/小说/古诗/文言文/作文 |
| 历史 | 中国古代史/中国近代史/世界古代史/世界近代史 |

### 8.2 难度标签

| 难度 | 说明 |
|------|------|
| easy | 基础概念/单一知识点 |
| medium | 综合性应用/多步骤推导 |
| hard | 难题/竞赛级别/跨学科综合 |

---

## 9. 与其他模块的接口

| 依赖模块 | 接口 | 说明 |
|---------|------|------|
| CourseGenerateService | `KnowledgePointInput getInputForCourse(Long kpId)` | 提供标准知识点输入 |
| 学习路径规划 | `List<KnowledgePoint> getPrerequisites(Long kpId)` | 提供前置依赖 |
| 资源统计 | `void updateResourceCount(Long kpId, int delta)` | 资源生成后更新计数 |

---

## 10. 实现计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 新增字段（subjectType, contentStructured, prerequisiteIds, estimatedMinutes） | P0 |
| Phase 2 | 创建/更新/删除接口（含关联课程检查） | P0 |
| Phase 3 | 知识点列表查询（按学科/年级/标签/难度过滤） | P0 |
| Phase 4 | 知识点详情（含关联课程+资源统计） | P1 |
| Phase 5 | 标签体系完善 + 自动打标签（LLM辅助） | P2 |
| Phase 6 | 知识图谱可视化（前置/后置关系图） | P2 |
