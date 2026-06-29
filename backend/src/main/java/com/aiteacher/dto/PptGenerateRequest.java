package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PptGenerateRequest {

    /** 生成模式: byCourse=基于课程(byCourseId) | byKeywords=基于关键字(byKnowledgePointId) */
    private String generationMode;

    // --- Mode: byCourse ---
    private Long courseId;

    // --- Mode: byKeywords ---
    private Long knowledgePointId;

    // --- Common ---
    /** 模板风格: default | elegant | minimal | vibrant | academic | hand-drawn-edu | chalkboard | kawaii | corporate-memphis */
    private String template;

    /** 授课风格: academic(学术) | vivid(生动) | concise(精炼) | academic(学术) */
    private String style;

    /** 学科类型: science(理科) | humanities(文科) */
    private String subjectType;

    // --- 用户输入: 关键字/重点 ---
    /** 用户强调的关键字列表，逗号分隔，如 "三角函数,图像变换" */
    private String keywords;

    /** 用户目标描述，如 "解题技巧" | "概念理解" | "考试复习" */
    private String goals;

    /** 目标受众: 小学生 | 初中生 | 高中生 | 大学生 | 成人 */
    private String targetAudience;

    /** 补充说明/备注，如 "需要联系生活实际" */
    private String additionalNotes;

    // --- AI Generation Control ---
    /** 难度等级: easy | medium | hard */
    private String difficultyLevel;

    /** 期望幻灯片数量 (默认8-15页) */
    private Integer slideCount;

    /** 授课语言: zh-CN | en (默认zh-CN) */
    private String language;

    /** 是否包含测验/问答环节 (默认true) */
    private Boolean includeQuiz;

    /** 是否包含活动页 (默认true) */
    private Boolean includeActivity;

    /** 是否包含知识漫画 (默认true) */
    private Boolean includeComic;

    /** 是否包含信息图 (默认true) */
    private Boolean includeInfographic;

    /** 是否包含互动节点 (默认true) */
    private Boolean includeInteraction;

    /** 互动密度: low | normal | high */
    private String interactionDensity;

    /** 每章幻灯片数量 (默认4-6) */
    private Integer slidesPerChapter;

    /** 美术风格: hand-drawn-edu | chalkboard | corporate-memphis | vintage | kawaii */
    private String artStyle;

    /** 动画风格: none | simple | moderate | rich (默认simple) */
    private String animationStyle;

    /** 章节数量 (默认3章) */
    private Integer chapterCount;

    /**
     * 指定使用的AI模型名称（可选）
     * 可选值: MiniMax | Claude | OpenAI | Qwen | DeepSeek
     * 不指定时自动选择优先级最高的可用模型
     */
    private String modelName;

    // --- Slide data (internal / Mode A) ---
    private String title;
    private String subtitle;
    private String subject;
    private String grade;
    private List<SlideData> slides;

    // --- Top-level fields from AI generation (Mode B) ---
    /** 叙事弧开场 */
    private String narrativeArcOpening;

    /** 叙事弧探索 */
    private String narrativeArcExploration;

    /** 叙事弧发现 */
    private String narrativeArcDiscovery;

    /** 叙事弧应用 */
    private String narrativeArcApplication;

    /** 学习目标列表 */
    private List<String> learningObjectives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlideData {
        /** 幻灯片类型: title | chapter | content | summary | end | text | diagram | case | quote | quiz | activity | reflection | comic | infographic | timeline | problem | concept | exercise | derivation | vote | poll | quick-fire | experiment | game | exit-ticket | result-viz */
        private String type;

        /** slideType alias — AI 输出用 slideType，Java DTO 用 type，JS 用 slideType */
        @JsonProperty("slideType")
        private String slideType;

        private String title;

        /** 主要阐述文字（100-300字） */
        private String mainBody;

        /** 主要内容（兼容旧字段） */
        private String content;

        /** 要点列表 */
        private List<String> contentList;

        private List<String> keyPoints;

        /** 随堂测验题列表 */
        private List<String> questions;

        /** 随堂测验（结构化） */
        private List<QuizQuestion> quizQuestions;

        private List<String> points;

        /** 该幻灯片关联的图片/信息图URL */
        private String imagePath;

        private String assetUrl;

        /** 建议时长（分钟） */
        private Integer duration;

        /** 教师备注（Speaker Notes） */
        private String teacherNote;

        /** 视觉建议（流程图、表格、漫画、信息图） */
        private String visualGuidance;

        /** 布局类型: bento | two-col | full-bleed | grid | hub-spoke | linear-progression | binary-comparison | hierarchical-layers | circular-flow | dense-modules */
        private String layout;

        /** 美术风格: hand-drawn | chalkboard | corporate-memphis | vintage | kawaii | ligne-claire */
        private String artStyle;

        /** 漫画分镜列表 */
        private List<ComicPanel> comicPanels;

        // --- 理科专属字段 ---
        /** 公式/表达式 */
        private String formula;

        /** 图示类型: flowchart | timeline | comparison | hierarchy */
        private String diagramType;

        /** 例题题目 */
        private String exampleProblem;

        /** 完整解题过程 */
        private String solution;

        /** 变式训练题列表 */
        private List<String> variants;

        /** 分步推导/证明步骤 */
        private List<String> steps;

        /** 推导结论 */
        private String conclusion;

        /** 案例/示例列表 */
        private List<String> examples;

        /** 名言内容 */
        private String quoteText;

        /** 名言作者 */
        private String quoteAuthor;

        /** 活动描述 */
        private String activityDesc;

        /** 思考/讨论题 */
        private String question;

        /** 快速问答答案 */
        private String answer;

        /** 游戏规则 */
        private List<String> rules;

        /** 时间线事件列表 */
        private List<TimelineEvent> timelineEvents;

        /** 统计摘要 */
        private String summary;

        // --- 章节级别字段（用于 chapter type slide） ---
        /** 章节时长（分钟） */
        private Integer chapterDuration;

        /** 章节学习目标 */
        private String learningObjectives;

        /** 章节叙事弧 */
        private String narrativeArc;

        /** 章节视觉主题 */
        private String visualTheme;

        // --- 章节结构（用于 group slides under a chapter） ---
        /** 所属章节ID */
        private Long chapterId;

        /** 所属章节标题 */
        private String chapterTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {
        /** 题目类型: choice | true-false | fill-blank | essay */
        private String type;

        private String question;

        /** 选项列表（如 A. xxx B. xxx） */
        private List<String> options;

        /** 正确答案 */
        private String answer;

        /** 答案解析 */
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComicPanel {
        /** 场景描述 */
        private String scene;

        /** 角色对话 */
        private String dialogue;

        /** 旁白说明 */
        private String caption;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private String year;
        private String label;
        private String event;
        private String description;
    }
}
