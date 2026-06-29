package com.aiteacher.service;

import com.aiteacher.dto.CourseOutline;
import com.aiteacher.dto.PptGenerateRequest;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * PPT Generation Service - generates PPT from course outline
 */
@Slf4j
@Service
public class PptGenerationService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AIService aiService;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ppt.node-script:./scripts/generate-ppt.js}")
    private String nodeScriptPath;

    @Value("${ppt.output-dir:/tmp/ppt}")
    private String outputDir;

    // ─────────────────────────────────────────────────────────────
    // Result DTO
    // ─────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PptGenerationResult {
        private TeachingMaterial material;
        private int totalSlides;
        private int totalDuration;
        private List<String> assetUrls;
        private List<ChapterSummary> chapters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterSummary {
        private Long chapterId;
        private String title;
        private int slideCount;
        private int duration;
        private List<SlideSummary> slides;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlideSummary {
        private String type;
        private String title;
        private Integer duration;
        private String assetUrl;
    }

    // ─────────────────────────────────────────────────────────────
    // Mode A: 基于已有课程 — AI 增强版（调用 AI 生成多样化幻灯片类型）
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate PPT from course (Mode A - AI-enhanced)
     * Calls AI to expand course outline into diverse slide types.
     */
    public PptGenerationResult generatePpt(Long courseId, String template, Long userId) {
        return generatePpt(courseId, template, userId, null);
    }

    /**
     * Generate PPT from course with optional model selection.
     */
    public PptGenerationResult generatePpt(Long courseId, String template, Long userId, String modelName) {
        try {
            // 1. Get course data
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                throw new RuntimeException("Course not found: " + courseId);
            }

            // 2. Parse outline
            CourseOutline outline = parseOutline(course.getOutline());

            // 3. Call AI to expand outline into diverse slides
            PptGenerateRequest aiConfig = generateSlidesFromCourseOutline(course, outline, template, modelName);

            // 4. Generate PPT file
            String outputPath = generatePptFile(aiConfig);

            // 5. Upload to file storage
            File pptFile = new File(outputPath);
            String fileUrl = uploadPptFile(pptFile, course.getTitle());

            // 6. Save teaching material
            TeachingMaterial material = saveTeachingMaterial(course, fileUrl, pptFile.length());

            // 7. Cleanup temp file
            Files.deleteIfExists(pptFile.toPath());

            // 8. Build result with metadata
            PptGenerationResult result = buildResultFromConfig(material, aiConfig);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate PPT: {}", e.getMessage(), e);
            throw new RuntimeException("PPT生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Build AI prompt for Mode A: course outline -> diverse slides.
     * Per-chapter generation: generates slides one chapter at a time to avoid token limits.
     * This avoids the issue where a single AI call for 27+ slides hits token limit mid-stream.
     */
    private PptGenerateRequest generateSlidesFromCourseOutline(Course course, CourseOutline outline, String template, String modelName) throws Exception {
        // Determine subject type (理科 vs 文科) from title/description
        String title = outline.getTitle() != null ? outline.getTitle() : course.getTitle();
        String subjectType = detectSubjectType(title, outline.getDescription());
        String artStyle = template != null ? template : "hand-drawn-edu";

        List<PptGenerateRequest.SlideData> allSlides = new ArrayList<>();

        // 1. Add title slide at the beginning
        allSlides.add(PptGenerateRequest.SlideData.builder()
                .type("title")
                .title(title)
                .mainBody(outline.getDescription() != null ? outline.getDescription() : "")
                .layout("full-bleed")
                .artStyle(artStyle)
                .duration(2)
                .build());

        // 2. Generate slides per chapter
        if (outline.getChapters() != null) {
            for (int i = 0; i < outline.getChapters().size(); i++) {
                CourseOutline.Chapter ch = outline.getChapters().get(i);

                // Add chapter header slide (programmatically, not from AI)
                allSlides.add(PptGenerateRequest.SlideData.builder()
                        .type("chapter")
                        .title(ch.getTitle())
                        .duration(ch.getDuration() != null ? ch.getDuration() : 5)
                        .layout("full-bleed")
                        .artStyle(artStyle)
                        .build());

                // Build per-chapter prompt and call AI
                String chapterPrompt = buildChapterPrompt(ch, subjectType, artStyle);
                List<PptGenerateRequest.SlideData> chapterSlides = callAIGenerateSlides(chapterPrompt, modelName);

                if (chapterSlides.isEmpty()) {
                    log.warn("Chapter {} '{}' returned no slides from AI, using fallback", i + 1, ch.getTitle());
                    // Fallback: add a basic content slide
                    List<String> contentList = new ArrayList<>();
                    if (ch.getKeyPoints() != null) contentList.addAll(ch.getKeyPoints());
                    if (ch.getTeachingNotes() != null) contentList.add(ch.getTeachingNotes());
                    chapterSlides.add(PptGenerateRequest.SlideData.builder()
                            .type("content")
                            .title(ch.getTitle())
                            .contentList(contentList)
                            .keyPoints(ch.getKeyPoints())
                            .layout("two-col")
                            .artStyle(artStyle)
                            .duration(ch.getDuration() != null ? ch.getDuration() : 5)
                            .build());
                }

                allSlides.addAll(chapterSlides);
                log.info("Generated {} slides for chapter {}: {}", chapterSlides.size(), i + 1, ch.getTitle());
            }
        }

        // 3. Add summary slide at the end
        List<String> chapterTitles = outline.getChapters() != null ?
                outline.getChapters().stream().map(CourseOutline.Chapter::getTitle).toList() :
                new ArrayList<>();
        allSlides.add(PptGenerateRequest.SlideData.builder()
                .type("summary")
                .title("课堂小结")
                .points(chapterTitles)
                .layout("bento")
                .artStyle(artStyle)
                .duration(2)
                .build());

        // 4. Build the result config
        PptGenerateRequest aiConfig = PptGenerateRequest.builder()
                .title(title)
                .subtitle(outline.getDescription())
                .template(artStyle)
                .courseId(course.getId())
                .slides(allSlides)
                .build();

        return aiConfig;
    }

    /**
     * Build a focused prompt for a single chapter.
     */
    private String buildChapterPrompt(CourseOutline.Chapter chapter, String subjectType, String artStyle) {
        String keyPoints = chapter.getKeyPoints() != null ? String.join("；", chapter.getKeyPoints()) : "无";
        String teachingNotes = chapter.getTeachingNotes() != null ? chapter.getTeachingNotes() : "无";
        int duration = chapter.getDuration() != null ? chapter.getDuration() : 10;

        return SINGLE_CHAPTER_SLIDES_PROMPT_TEMPLATE
                .replace("PH_SUBJECT_TYPE", subjectType)
                .replace("PH_ART_STYLE", artStyle)
                .replace("PH_CHAPTER_TITLE", chapter.getTitle() != null ? chapter.getTitle() : "")
                .replace("PH_KEY_POINTS", keyPoints)
                .replace("PH_TEACHING_NOTES", teachingNotes)
                .replace("PH_CHAPTER_DURATION", String.valueOf(duration));
    }

    /**
     * Call AI to generate slides for a single chapter.
     * Returns parsed slides array or empty list on parse error (graceful degradation).
     */
    private List<PptGenerateRequest.SlideData> callAIGenerateSlides(String prompt, String modelName) {
        try {
            log.info("Calling AI for chapter slides (model={}), prompt length: {}", modelName, prompt.length());
            ChatRequest aiRequest = ChatRequest.builder()
                    .messages(List.of(ChatMessage.user(prompt)))
                    .maxTokens(20000)
                    .build();
            ChatResponse aiResponse = (modelName != null && !modelName.isEmpty())
                    ? aiService.chat(aiRequest, modelName)
                    : aiService.chat(aiRequest);
            String rawContent = aiResponse.getContent().trim();
            rawContent = stripMarkdownCodeBlocks(rawContent);

            log.info("Chapter AI raw response (first 500 chars): {}", rawContent.substring(0, Math.min(500, rawContent.length())));

            // Parse the response - AI returns {"slides": [...]}
            try {
                // First try direct parse as PptGenerateRequest
                PptGenerateRequest tempConfig = objectMapper.readValue(rawContent, PptGenerateRequest.class);
                if (tempConfig.getSlides() != null) {
                    // Normalize slideType -> type
                    for (PptGenerateRequest.SlideData slide : tempConfig.getSlides()) {
                        if ((slide.getType() == null || slide.getType().isEmpty()) && slide.getSlideType() != null) {
                            slide.setType(slide.getSlideType());
                        }
                    }
                    return tempConfig.getSlides();
                }
            } catch (Exception parseEx) {
                log.warn("Direct parse failed, trying to extract slides from response: {}", parseEx.getMessage());
            }

            // Try to fix truncated JSON
            String fixed = tryFixTruncatedJson(rawContent);
            if (fixed != null) {
                try {
                    PptGenerateRequest tempConfig = objectMapper.readValue(fixed, PptGenerateRequest.class);
                    if (tempConfig.getSlides() != null) {
                        for (PptGenerateRequest.SlideData slide : tempConfig.getSlides()) {
                            if ((slide.getType() == null || slide.getType().isEmpty()) && slide.getSlideType() != null) {
                                slide.setType(slide.getSlideType());
                            }
                        }
                        return tempConfig.getSlides();
                    }
                } catch (Exception e2) {
                    log.warn("Failed to parse fixed JSON: {}", e2.getMessage());
                }
            }

            // Last resort: try to extract slides array directly using bracket matching
            List<PptGenerateRequest.SlideData> extracted = extractSlidesArray(rawContent);
            if (extracted != null && !extracted.isEmpty()) {
                return extracted;
            }

            log.error("Failed to parse AI response for slides, returning empty list");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract slides array from potentially truncated/broken JSON.
     * Finds the first "slides": [ ... ] block using proper bracket matching,
     * then wraps it in {"slides": [...]} and parses.
     * Handles cases where other top-level fields contain nested arrays
     * (e.g., narrativeArc, learningObjectives) that confuse simple bracket matching.
     */
    private List<PptGenerateRequest.SlideData> extractSlidesArray(String raw) {
        try {
            // Find "slides": [
            int slidesIdx = raw.indexOf("\"slides\": [");
            if (slidesIdx < 0) {
                // Try alternate form
                slidesIdx = raw.indexOf("\"slides\":[");
            }
            if (slidesIdx < 0) return null;

            // Find opening bracket after "slides":
            int bracketStart = raw.indexOf("[", slidesIdx);
            if (bracketStart < 0) return null;

            // Find matching closing bracket using depth counting
            int depth = 0;
            int i = bracketStart;
            boolean inString = false;
            while (i < raw.length()) {
                char c = raw.charAt(i);
                if (c == '"' && (i == 0 || raw.charAt(i-1) != '\\')) {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            String slidesContent = raw.substring(bracketStart, i + 1);
                            if (slidesContent.length() < 20) return null;

                            String wrapper = "{\"slides\":" + slidesContent + "}";
                            try {
                                PptGenerateRequest temp = objectMapper.readValue(wrapper, PptGenerateRequest.class);
                                if (temp.getSlides() != null && !temp.getSlides().isEmpty()) {
                                    for (PptGenerateRequest.SlideData slide : temp.getSlides()) {
                                        if ((slide.getType() == null || slide.getType().isEmpty()) && slide.getSlideType() != null) {
                                            slide.setType(slide.getSlideType());
                                        }
                                    }
                                    return temp.getSlides();
                                }
                            } catch (Exception e) {
                                log.debug("extractSlidesArray parse failed: {}", e.getMessage());
                            }
                            return null;
                        }
                    }
                }
                i++;
            }
        } catch (Exception e) {
            log.debug("extractSlidesArray failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Per-chapter generation: generate slides for ONE chapter at a time.
     * This avoids token limit issues when generating 27+ slides for 3 chapters.
     */
    private static final String SINGLE_CHAPTER_SLIDES_PROMPT_TEMPLATE = """
            【系统提示】
            你是资深教育专家，精通以下领域：
            1. K-12 各学科知识（数学/物理/化学/生物/历史/语文/政治）
            2. 教学设计：Bloom认知分类、建构主义学习、掌握学习法
            3. PPT设计原则：每页一个核心信息、文字密度≤20%%、必须有视觉元素
            4. 理科（数学/物理/化学）要求：准确性优先，公式必须完整带单位，不能跳步

            【角色约束】
            - 学科类型：PH_SUBJECT_TYPE（science=理科 | humanities=文科，由系统根据科目判断）
            - 美术风格：PH_ART_STYLE（hand-drawn-edu=手绘风 | chalkboard=黑板风 | corporate-memphis=孟菲斯风 | kawaii=可爱风 | vintage=复古风）

            【任务】
            基于以下章节大纲，为该章节设计9-12张结构完整、内容专业的PPT幻灯片。
            注意：不需要生成chapter类型封面页（会在程序中自动添加）。

            【章节基本信息】
            标题：PH_CHAPTER_TITLE
            核心要点：PH_KEY_POINTS
            教学备注：PH_TEACHING_NOTES
            章节时长：PH_CHAPTER_DURATION分钟

            【幻灯片类型要求 — 必须使用以下类型，禁止只用title/chapter/content/summary/end】
            本章必须生成9-12张幻灯片，包含以下类型（按推荐顺序）：
            - problem（问题引入，1次）：用生活问题/实验现象引入，≤3行文字
            - concept（概念讲解，2-4次）：揭示核心原理/公式，配合要点，≤3行文字
            - derivation（公式推导/证明，数学/物理必选）：分步推导，含完整符号和单位
            - exercise（例题+变式题，2-3次）：典型例题→分步解答→变式训练
            - infographic（知识结构可视化，至少1次）：知识结构图/思维导图
            - comic（概念理解漫画，至少1次）：2-4格漫画解释核心概念
            - quiz（随堂测验，至少1次）：结构化题目
            - vote/poll（课堂投票/快速统计）：互动检验
            - exit-ticket（出门票，1次）：章节结束时总结性问题

            【幻灯片内容约束】
            1. 每张幻灯片的mainBody不超过100字，bullets不超过3条
            2. 理科的formula字段必须包含完整数学表达式（含所有符号和单位）
            3. 每张幻灯片必须包含visualGuidance字段（描述需要的视觉元素）
            4. 每张幻灯片必须包含layout字段（bento | two-col | full-bleed | grid | hub-spoke | linear-progression | binary-comparison | hierarchical-layers | circular-flow | dense-modules）
            5. 每张幻灯片必须包含artStyle字段（继承全局artStyle）
            6. 漫画类幻灯片必须包含comicPanels字段（分镜描述：scene + dialogue + caption）
            7. 测验类幻灯片必须包含quizQuestions字段（type + question + options + answer + explanation）
            8. 视觉型幻灯片（infographic + diagram + comic）占比不低于55%%

            【输出格式】
            直接输出JSON，不要有任何前缀或解释，不要用markdown代码块包裹：
            {
              "slides": [
                {
                  "slideType": "problem",
                  "title": "问题引入标题",
                  "mainBody": "生活中的问题描述（≤100字）",
                  "scenario": "情境描述",
                  "visualGuidance": "建议的视觉元素",
                  "layout": "full-bleed",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 2,
                  "teacherNote": "老师操作提示"
                },
                {
                  "slideType": "concept",
                  "title": "核心概念标题",
                  "mainBody": "概念阐述（≤100字）",
                  "bullets": ["要点1", "要点2", "要点3"],
                  "formula": "公式（理科必须）",
                  "visualGuidance": "视觉元素建议",
                  "layout": "two-col",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 3,
                  "teacherNote": "老师操作提示"
                },
                {
                  "slideType": "derivation",
                  "title": "公式推导",
                  "mainBody": "推导说明（≤80字）",
                  "steps": ["步骤1", "步骤2", "步骤3"],
                  "conclusion": "推导结论",
                  "visualGuidance": "分步展示推导过程",
                  "layout": "linear-progression",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 5,
                  "teacherNote": "引导学生跟随思考"
                },
                {
                  "slideType": "exercise",
                  "title": "例题演练标题",
                  "mainBody": "题目描述（≤80字）",
                  "exampleProblem": "例题完整题目（含已知条件+求解）",
                  "solution": "完整解题步骤（分步说明）",
                  "variants": ["变式题1", "变式题2", "变式题3"],
                  "visualGuidance": "板书演示或动画展示",
                  "layout": "two-col",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 5,
                  "teacherNote": "老师操作提示"
                },
                {
                  "slideType": "infographic",
                  "title": "知识结构图",
                  "mainBody": "一句话概括（≤50字）",
                  "bullets": ["结构要点1", "结构要点2", "结构要点3", "结构要点4"],
                  "visualGuidance": "hub-spoke或bento布局的信息图",
                  "layout": "bento",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 3,
                  "teacherNote": "可配合思维导图使用"
                },
                {
                  "slideType": "comic",
                  "title": "概念理解漫画",
                  "comicPanels": [
                    { "scene": "场景1描述", "dialogue": "角色对话1", "caption": "旁白说明1" },
                    { "scene": "场景2描述", "dialogue": "角色对话2", "caption": "旁白说明2" },
                    { "scene": "场景3描述", "dialogue": "角色对话3", "caption": "旁白说明3" }
                  ],
                  "visualGuidance": "2-4格漫画，用于解释核心概念",
                  "layout": "grid",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 3,
                  "teacherNote": "可让学生分角色朗读对话"
                },
                {
                  "slideType": "quiz",
                  "title": "随堂测验",
                  "quizQuestions": [
                    {
                      "type": "choice",
                      "question": "测验题目",
                      "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                      "answer": "A",
                      "explanation": "答案解析"
                    }
                  ],
                  "visualGuidance": "题目居中+选项卡片",
                  "layout": "full-bleed",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 3
                },
                {
                  "slideType": "exit-ticket",
                  "title": "出门票",
                  "question": "总结性问题",
                  "visualGuidance": "问题居中+空白答题区",
                  "layout": "full-bleed",
                  "artStyle": "PH_ART_STYLE",
                  "duration": 2,
                  "teacherNote": "学生回答后可作为课后作业收集"
                }
              ]
            }

            【格式要求 - 必须遵守】
            - 直接输出JSON，不要有任何前缀或解释
            - 不要用markdown代码块包裹
            - 必须生成9-12张幻灯片
            - 必须包含problem/concept/exercise/derivation类型
            - exercise类型必须包含exampleProblem（完整例题）、solution（分步解答）、variants（2-3道变式题）
            - 每张幻灯片必须有visualGuidance和layout字段
            - 如违反格式，本次回答将被判定为失败
            """;


    // ─────────────────────────────────────────────────────────────
    // Mode B: 基于知识点 + 用户输入关键字
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // Enhanced Prompt Template — 遵循 ppt-generation-design.md
    // ─────────────────────────────────────────────────────────────

    private static final String KEYWORDS_TO_SLIDES_PROMPT_TEMPLATE = """
            【系统提示】
            你是资深教育专家，精通以下领域：
            1. K-12 各学科知识（数学/物理/化学/生物/历史/语文/政治）
            2. 教学设计：Bloom认知分类、建构主义学习、掌握学习法
            3. PPT设计原则：每页一个核心信息、文字密度≤20%、必须有视觉元素
            4. 理科（数学/物理/化学）要求：准确性优先，公式必须完整带单位，不能跳步

            【角色约束】
            - 学科类型：%s（science=理科 | humanities=文科，由系统根据科目判断）
            - 科目：%s
            - 学段：%s（影响内容深度）
            - 目标受众：%s
            - 授课语言：%s

            【任务】
            为知识点「%s」设计一套结构完整、内容专业的PPT幻灯片。

            【用户需求】
            - 用户强调的关键字（必须高频呈现）：%s
            - 用户目标：%s
            - 补充说明：%s

            【AI生成控制参数】
            - 难度等级：%s（easy=基础概念趣味为主 | medium=兼顾概念与应用 | hard=包含原理分析公式推导）
            - 期望幻灯片数量：%d页（实际生成不少于%d页）
            - 章节数量：%d章
            - 是否包含测验/问答：%s
            - 动画风格：%s
            - 美术风格：%s（hand-drawn-edu=手绘风 | chalkboard=黑板风 | corporate-memphis=孟菲斯风 | kawaii=可爱风 | vintage=复古风）

            【叙事弧设计 — 必须执行】
            在生成幻灯片之前，为此课程设计一个整体"叙事弧"：
            - opening（开场）：用生活中的真实问题/现象引入，激发好奇心
            - exploration（探索）：引导学生逐步理解核心概念
            - discovery（发现）：核心原理/规律的揭示
            - application（应用）：知识在实际生活中的应用

            【理科四步法 — 理科必须严格遵循】
            每章必须包含以下四个步骤（不可跳过）：
            Step 1：问题引入（problem类型）- 用生活问题/实验现象引入，≤3行文字
            Step 2：概念讲解（concept类型）- 揭示核心原理/公式，配合要点，≤3行文字
            Step 3：例题演练（exercise类型）- 典型例题→分步解答→变式训练，例题必须有完整解答
            Step 4：总结提升（summary/exit-ticket类型）- 一句话核心结论 + 知识结构图 + 课后思考

            【互动节点设计 — 每章至少3个】
            在叙事弧的3个关键节点各插入1个互动：
            - 导入环节（开场问题之后）→ vote 或 poll（检验预习效果）
            - 概念讲解后 → quick-fire 或 think-pair-share（检验即时理解）
            - 练习结束后 → exit-ticket 或 game（检验知识掌握）
            每个互动节点必须包含：
            - "interactionType": "vote | poll | quick-fire | think-pair-share | experiment | game | exit-ticket"
            - "question": 互动的问题内容
            - "triggerMoment": 触发时机
            - "expectedDuration": 预计时长（分钟）
            - "teacherNote": 老师操作提示

            【幻灯片类型要求 — 必须使用以下类型，禁止只用title/chapter/content/summary/end】
            理科必须包含（按出现顺序）：
            - problem（问题引入，每章1次）
            - concept（概念讲解，每章2-4次）
            - derivation（公式推导/证明，数学/物理必选）
            - exercise（例题+变式题，每章2-3次）
            - infographic（知识结构可视化，每章至少1次）
            - comic（概念理解漫画，每章至少1次）
            - quiz（随堂测验，每章至少1次）
            - vote/poll（课堂投票/快速统计）
            - quick-fire（快速问答）
            - exit-ticket（出门票，每章结束时）
            - summary（章节总结，每章1次）

            文科必须包含（按出现顺序）：
            - infographic（知识结构/对比/分类可视化，每章至少2次）
            - comic（故事情境/人物案例漫画，每章至少1次）
            - diagram（流程图/对比图/关系图，每章至少1次）
            - case（生活案例分析，每章至少1次）
            - quote（名人名言，每章至少1次）
            - quiz（随堂测验，每章至少1次）
            - reflection（思考讨论题，每章至少1次）
            - activity（课堂互动活动，每章至少1次）
            - timeline（历史/发展时间线，历史类课程至少1次）
            - vote/poll（课堂投票/快速统计）
            - exit-ticket（出门票，每章结束时）
            - summary（章节总结，每章1次）

            【幻灯片内容约束】
            1. 每张幻灯片的mainBody不超过100字，bullets不超过3条
            2. 理科的formula字段必须包含完整数学表达式（含所有符号和单位）
            3. 每张幻灯片必须包含visualGuidance字段（描述需要的视觉元素，如"流程图"、"对比表格"、"漫画分镜"、"信息图"）
            4. 每张幻灯片必须包含layout字段（bento | two-col | full-bleed | grid | hub-spoke | linear-progression | binary-comparison | hierarchical-layers | circular-flow | dense-modules）
            5. 每张幻灯片必须包含artStyle字段（继承全局artStyle或根据内容调整）
            6. 漫画类幻灯片必须包含comicPanels字段（分镜描述：scene + dialogue + caption）
            7. 测验类幻灯片必须包含quizQuestions字段（结构化题目：type + question + options + answer + explanation）
            8. 视觉型幻灯片（infographic + diagram + comic）占比不低于55%%

            【输出格式】
            直接输出JSON，不要有任何前缀或解释，不要用markdown代码块包裹：
            {
              "title": "PPT标题",
              "subtitle": "副标题",
              "narrativeArc": {
                "opening": "开场问题描述",
                "exploration": "探索阶段描述",
                "discovery": "发现时刻描述",
                "application": "应用阶段描述"
              },
              "learningObjectives": ["学习目标1", "学习目标2", "学习目标3"],
              "slides": [
                {
                  "slideType": "chapter",
                  "title": "章节1标题",
                  "mainBody": "章节简介（≤50字）",
                  "visualGuidance": "章节封面图",
                  "layout": "full-bleed",
                  "artStyle": "hand-drawn-edu",
                  "duration": 3,
                  "teacherNote": "老师操作提示"
                },
                {
                  "slideType": "problem",
                  "title": "问题引入标题",
                  "mainBody": "生活中的问题描述（≤100字）",
                  "scenario": "情境描述",
                  "visualGuidance": "建议的视觉元素",
                  "layout": "full-bleed",
                  "artStyle": "hand-drawn-edu",
                  "duration": 2,
                  "teacherNote": "老师操作提示"
                },
                {
                  "slideType": "concept",
                  "title": "核心概念标题",
                  "mainBody": "概念阐述（≤100字）",
                  "bullets": ["要点1", "要点2", "要点3"],
                  "formula": "完整公式（含单位）",
                  "visualGuidance": "概念图或公式图示",
                  "layout": "two-col",
                  "artStyle": "hand-drawn-edu",
                  "duration": 5,
                  "teacherNote": "老师讲解提示"
                },
                {
                  "slideType": "exercise",
                  "title": "例题演练",
                  "exampleProblem": "例题题目",
                  "solution": "完整解题过程",
                  "variants": ["变式题1", "变式题2", "变式题3"],
                  "visualGuidance": "题目展示+分步解答",
                  "layout": "two-col",
                  "artStyle": "hand-drawn-edu",
                  "duration": 8,
                  "teacherNote": "先让学生思考，再公布解答"
                },
                {
                  "slideType": "infographic",
                  "title": "知识结构图",
                  "mainBody": "一句话概括（≤50字）",
                  "bullets": ["结构要点1", "结构要点2", "结构要点3", "结构要点4"],
                  "visualGuidance": "hub-spoke或bento布局的信息图",
                  "layout": "bento",
                  "artStyle": "corporate-memphis",
                  "duration": 3,
                  "teacherNote": "可配合思维导图使用"
                },
                {
                  "slideType": "comic",
                  "title": "概念理解漫画",
                  "comicPanels": [
                    { "scene": "场景1描述", "dialogue": "角色对话1", "caption": "旁白说明1" },
                    { "scene": "场景2描述", "dialogue": "角色对话2", "caption": "旁白说明2" },
                    { "scene": "场景3描述", "dialogue": "角色对话3", "caption": "旁白说明3" }
                  ],
                  "visualGuidance": "2-4格漫画，用于解释核心概念",
                  "layout": "grid",
                  "artStyle": "hand-drawn-edu",
                  "duration": 3,
                  "teacherNote": "可让学生分角色朗读对话"
                },
                {
                  "slideType": "quiz",
                  "title": "随堂测验",
                  "quizQuestions": [
                    {
                      "type": "choice",
                      "question": "测验题目",
                      "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                      "answer": "A",
                      "explanation": "答案解析"
                    }
                  ],
                  "visualGuidance": "题目居中+选项卡片",
                  "layout": "full-bleed",
                  "artStyle": "hand-drawn-edu",
                  "duration": 3
                },
                {
                  "slideType": "vote",
                  "title": "课堂投票",
                  "question": "投票问题",
                  "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                  "visualGuidance": "大字问题居中+彩色选项卡片",
                  "layout": "full-bleed",
                  "artStyle": "corporate-memphis",
                  "duration": 2,
                  "teacherNote": "点击开始投票，实时展示柱状图结果"
                },
                {
                  "slideType": "exit-ticket",
                  "title": "出门票",
                  "question": "总结性问题",
                  "visualGuidance": "问题居中+空白答题区",
                  "layout": "full-bleed",
                  "artStyle": "hand-drawn-edu",
                  "duration": 2,
                  "teacherNote": "学生回答后可作为课后作业收集"
                },
                {
                  "slideType": "summary",
                  "title": "章节小结",
                  "mainBody": "一句话总结（≤50字）",
                  "bullets": ["本节要点1", "本节要点2", "本节要点3"],
                  "visualGuidance": "知识结构图或思维导图",
                  "layout": "bento",
                  "artStyle": "hand-drawn-edu",
                  "duration": 2
                }
              ],
              "totalDuration": PH_TOTAL_DURATION
            }

            【格式要求 - 必须遵守】
            - 直接输出JSON，不要有任何前缀或解释
            - 不要用```json或```包裹
            - slides数组至少包含%d个元素
            - 理科必须包含problem/concept/exercise/derivation/summary类型
            - 文科必须包含infographic/comic/case/quote/quiz/reflection类型
            - 每个幻灯片必须有visualGuidance和layout字段
            - 如违反格式，本次回答将被判定为失败
            """;

    /**
     * Generate PPT from knowledge point + user keywords (Mode B)
     */
    public PptGenerationResult generatePptFromKeywords(PptGenerateRequest request, Long userId) {
        try {
            // 1. Get knowledge point
            KnowledgePoint kp = knowledgePointMapper.selectById(request.getKnowledgePointId());
            if (kp == null) {
                throw new RuntimeException("知识点不存在: " + request.getKnowledgePointId());
            }

            // 2. Build AI prompt
            String prompt = buildKeywordsPrompt(kp, request);

            // 3. Call AI to generate slide content
            log.info("Calling AI to generate PPT slides from keywords for knowledge point: {} (model={})", kp.getId(), request.getModelName());
            ChatResponse aiResponse = (request.getModelName() != null && !request.getModelName().isEmpty())
                    ? aiService.chat(ChatRequest.of(prompt), request.getModelName())
                    : aiService.chat(ChatRequest.of(prompt));
            String rawContent = aiResponse.getContent().trim();

            // Strip markdown code blocks
            rawContent = stripMarkdownCodeBlocks(rawContent);

            log.info("PPT AI raw response (first 800 chars): {}", rawContent.substring(0, Math.min(800, rawContent.length())));

            // Parse AI response
            PptGenerateRequest aiConfig = objectMapper.readValue(rawContent, PptGenerateRequest.class);
            log.info("PPT AI parsed config - title={}, slidesCount={}",
                aiConfig.getTitle(),
                aiConfig.getSlides() != null ? aiConfig.getSlides().size() : 0);

            // Normalize slideType -> type (AI outputs slideType, Java DTO uses type)
            if (aiConfig.getSlides() != null) {
                for (PptGenerateRequest.SlideData slide : aiConfig.getSlides()) {
                    if ((slide.getType() == null || slide.getType().isEmpty()) && slide.getSlideType() != null) {
                        slide.setType(slide.getSlideType());
                    }
                }
            }

            // Extract narrativeArc from AI JSON (AI returns nested object, we flattened to top-level fields)
            // The AI returns: "narrativeArc": {"opening": "...", "exploration": "...", "discovery": "...", "application": "..."}
            // We need to parse it from the raw JSON directly since our DTO doesn't have a nested NarrativeArc class
            extractNarrativeArc(rawContent, aiConfig);

            // Also parse learningObjectives from raw JSON (AI returns it as a top-level array)
            extractLearningObjectives(rawContent, aiConfig);

            // Merge user inputs that AI might not have respected
            if (request.getTemplate() != null) {
                aiConfig.setTemplate(request.getTemplate());
            } else {
                aiConfig.setTemplate("default");
            }
            aiConfig.setCourseId(request.getKnowledgePointId());

            // 4. Generate PPT file
            String outputPath = generatePptFile(aiConfig);

            // 5. Upload
            File pptFile = new File(outputPath);
            String fileUrl = uploadPptFile(pptFile, aiConfig.getTitle() != null ? aiConfig.getTitle() : kp.getContent());

            // 6. Save teaching material
            TeachingMaterial material = new TeachingMaterial();
            material.setKnowledgePointId(request.getKnowledgePointId());
            material.setTenantId(kp.getTenantId());
            material.setMaterialType("ppt");
            material.setTitle(aiConfig.getTitle() != null ? aiConfig.getTitle() : kp.getContent());
            material.setFileUrl(fileUrl);
            material.setFileSize(pptFile.length());
            material.setStatus("generated");
            material.setCreatedAt(java.time.LocalDateTime.now());
            teachingMaterialMapper.insert(material);

            // 7. Cleanup
            Files.deleteIfExists(pptFile.toPath());

            // 8. Build result with metadata
            PptGenerationResult result = buildResultFromConfig(material, aiConfig);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate PPT from keywords: {}", e.getMessage(), e);
            throw new RuntimeException("PPT生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Build PptGenerationResult from the config (counts slides, durations, assetUrls)
     */
    private PptGenerationResult buildResultFromConfig(TeachingMaterial material, PptGenerateRequest config) {
        List<PptGenerateRequest.SlideData> slides = config.getSlides();
        int totalSlides = slides != null ? slides.size() : 0;
        int totalDuration = 0;
        List<String> assetUrls = new ArrayList<>();
        List<ChapterSummary> chapters = new ArrayList<>();

        if (slides != null) {
            int currentChapterIdx = -1;
            ChapterSummary currentChapter = null;

            for (PptGenerateRequest.SlideData slide : slides) {
                // Sum duration
                if (slide.getDuration() != null) {
                    totalDuration += slide.getDuration();
                }

                // Collect assetUrls
                if (slide.getAssetUrl() != null && !slide.getAssetUrl().isEmpty()) {
                    assetUrls.add(slide.getAssetUrl());
                }

                // Track chapters
                if ("chapter".equals(slide.getType()) || "chapter".equals(slide.getSlideType())) {
                    currentChapterIdx++;
                    currentChapter = ChapterSummary.builder()
                            .chapterId((long) currentChapterIdx)
                            .title(slide.getTitle())
                            .slideCount(0)
                            .duration(slide.getDuration() != null ? slide.getDuration() : 0)
                            .slides(new ArrayList<>())
                            .build();
                    chapters.add(currentChapter);
                } else if (currentChapter != null) {
                    // Add slide to current chapter
                    String slideTypeValue = slide.getType() != null ? slide.getType() : slide.getSlideType();
                    currentChapter.getSlides().add(SlideSummary.builder()
                            .type(slideTypeValue)
                            .title(slide.getTitle())
                            .duration(slide.getDuration())
                            .assetUrl(slide.getAssetUrl())
                            .build());
                    currentChapter.setSlideCount(currentChapter.getSlideCount() + 1);
                    if (slide.getDuration() != null) {
                        currentChapter.setDuration(currentChapter.getDuration() + slide.getDuration());
                    }
                }
            }
        }

        return PptGenerationResult.builder()
                .material(material)
                .totalSlides(totalSlides)
                .totalDuration(totalDuration)
                .assetUrls(assetUrls)
                .chapters(chapters)
                .build();
    }

    private String buildKeywordsPrompt(KnowledgePoint kp, PptGenerateRequest request) {
        String keywords = request.getKeywords() != null ? request.getKeywords() : "";
        String goals = request.getGoals() != null ? request.getGoals() : "知识讲解";
        String audience = request.getTargetAudience() != null ? request.getTargetAudience() : (kp.getGrade() != null ? kp.getGrade() : "中学生");
        String notes = request.getAdditionalNotes() != null ? request.getAdditionalNotes() : "";

        // AI generation control parameters
        String difficultyLevel = request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "medium";
        int slideCount = request.getSlideCount() != null ? request.getSlideCount() : 12;
        String language = request.getLanguage() != null ? request.getLanguage() : "zh-CN";
        Boolean includeQuiz = request.getIncludeQuiz() != null ? request.getIncludeQuiz() : true;
        String animationStyle = request.getAnimationStyle() != null ? request.getAnimationStyle() : "simple";
        int chapterCount = request.getChapterCount() != null ? request.getChapterCount() : 3;
        String artStyle = request.getArtStyle() != null ? request.getArtStyle() : "hand-drawn-edu";

        // Infer subject type from subject name (crude but works)
        String subject = kp.getSubject() != null ? kp.getSubject() : "通用";
        String subjectType = "science"; // default
        if (subject.contains("历史") || subject.contains("语文") || subject.contains("政治") ||
            subject.contains("地理") || subject.contains("英语") || subject.contains("美术") ||
            subject.contains("音乐") || subject.contains("思想")) {
            subjectType = "humanities";
        }

        return String.format(
                KEYWORDS_TO_SLIDES_PROMPT_TEMPLATE,
                // 【角色约束】
                subjectType,           // %s 学科类型：science | humanities
                subject,               // %s 科目
                kp.getGrade() != null ? kp.getGrade() : "通用",  // %s 学段
                audience,              // %s 目标受众
                language,              // %s 授课语言
                // 【任务】
                kp.getContent(),       // %s 知识点
                // 【用户需求】
                keywords,              // %s 关键字
                goals,                 // %s 用户目标
                notes,                  // %s 补充说明
                // 【AI生成控制参数】
                difficultyLevel,        // %s 难度等级
                slideCount,            // %d 推荐幻灯片数量
                slideCount,            // %d 最少幻灯片数量
                chapterCount,          // %d 章节数量
                includeQuiz ? "是" : "否",  // %s 是否包含测验
                animationStyle,        // %s 动画风格
                artStyle,              // %s 美术风格
                // 【输出格式】
                slideCount * 3,       // %d totalDuration（估算：每页3分钟）
                slideCount             // %d slides数组至少包含N个元素
        );
    }

    private String stripMarkdownCodeBlocks(String content) {
        String result = content.trim();

        // Remove markdown code block wrappers
        if (result.startsWith("```json")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }

        result = result.trim();

        // If still starts with # markdown header, try to extract JSON from the content
        // The AI sometimes returns markdown with headers instead of pure JSON
        if (result.startsWith("#")) {
            // Try to find JSON object or array in the content
            int jsonStart = result.indexOf('{');
            int jsonEnd = result.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                result = result.substring(jsonStart, jsonEnd + 1);
            } else {
                int arrStart = result.indexOf('[');
                int arrEnd = result.lastIndexOf(']');
                if (arrStart >= 0 && arrEnd > arrStart) {
                    result = result.substring(arrStart, arrEnd + 1);
                }
            }
        }

        return result.trim();
    }

    /**
     * Generate PPT from request directly
     */
    public PptGenerationResult generatePptFromRequest(PptGenerateRequest request, Long userId) {
        try {
            // 1. Generate PPT
            String outputPath = generatePptFile(request);

            // 2. Upload to file storage
            File pptFile = new File(outputPath);
            String fileUrl = uploadPptFile(pptFile, request.getTitle());

            // 3. Save teaching material
            TeachingMaterial material = new TeachingMaterial();
            material.setCourseId(request.getCourseId());
            material.setMaterialType("ppt");
            material.setTitle(request.getTitle());
            material.setFileUrl(fileUrl);
            material.setFileSize(pptFile.length());
            material.setStatus("generated");
            material.setCreatedAt(java.time.LocalDateTime.now());

            teachingMaterialMapper.insert(material);

            // 4. Cleanup
            Files.deleteIfExists(pptFile.toPath());

            // 5. Build result
            PptGenerationResult result = buildResultFromConfig(material, request);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate PPT: {}", e.getMessage(), e);
            throw new RuntimeException("PPT生成失败: " + e.getMessage(), e);
        }
    }

    private CourseOutline parseOutline(String outlineJson) {
        try {
            return objectMapper.readValue(outlineJson, CourseOutline.class);
        } catch (Exception e) {
            log.warn("Failed to parse outline, using default: {}", e.getMessage());
            CourseOutline outline = new CourseOutline();
            outline.setTitle("课程");
            outline.setChapters(new ArrayList<>());
            return outline;
        }
    }

    private PptGenerateRequest buildPptConfig(Course course, CourseOutline outline, String template) {
        List<PptGenerateRequest.SlideData> slides = new ArrayList<>();

        // 1. Title slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("title")
                .build());

        // 2. Chapter slides
        if (outline.getChapters() != null) {
            for (int i = 0; i < outline.getChapters().size(); i++) {
                CourseOutline.Chapter chapter = outline.getChapters().get(i);

                // Chapter header
                slides.add(PptGenerateRequest.SlideData.builder()
                        .type("chapter")
                        .title(chapter.getTitle())
                        .duration(chapter.getDuration())
                        .build());

                // Content slide
                List<String> contentList = new ArrayList<>();
                if (chapter.getKeyPoints() != null) {
                    contentList.addAll(chapter.getKeyPoints());
                }
                if (chapter.getTeachingNotes() != null) {
                    contentList.add(chapter.getTeachingNotes());
                }

                slides.add(PptGenerateRequest.SlideData.builder()
                        .type("content")
                        .title(chapter.getTitle())
                        .contentList(contentList)
                        .keyPoints(chapter.getKeyPoints())
                        .build());
            }
        }

        // 3. Summary slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("summary")
                .title("课堂小结")
                .points(outline.getChapters() != null ?
                        outline.getChapters().stream().map(CourseOutline.Chapter::getTitle).toList() :
                        new ArrayList<>())
                .build());

        // 4. End slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("end")
                .build());

        return PptGenerateRequest.builder()
                .title(outline.getTitle() != null ? outline.getTitle() : course.getTitle())
                .subtitle(outline.getDescription())
                .template(template != null ? template : "default")
                .slides(slides)
                .courseId(course.getId())
                .build();
    }

    private String generatePptFile(PptGenerateRequest config) throws Exception {
        // Create output directory
        Path outputPath = Path.of(outputDir);
        Files.createDirectories(outputPath);

        // Create temp config file
        String configFile = outputDir + "/config_" + System.currentTimeMillis() + ".json";
        String outputFile = outputDir + "/ppt_" + System.currentTimeMillis() + ".pptx";

        try {
            // Write config to file
            String configJson = objectMapper.writeValueAsString(config);
            Files.writeString(Path.of(configFile), configJson);
            log.info("PPT config JSON (first 500 chars): {}", configJson.substring(0, Math.min(500, configJson.length())));

            // Execute node script
            ProcessBuilder pb = new ProcessBuilder(
                    "node",
                    nodeScriptPath,
                    outputFile,
                    configFile
            );
            pb.environment().put("NODE_PATH", "/usr/local/lib/node_modules");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("PPT generation: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("PPT generation script failed with exit code: " + exitCode);
            }

            // Check output file exists
            if (!Files.exists(Path.of(outputFile))) {
                throw new RuntimeException("PPT file was not generated");
            }

            return outputFile;

        } finally {
            // Cleanup config file
            Files.deleteIfExists(Path.of(configFile));
        }
    }

    private String uploadPptFile(File file, String title) {
        try {
            // uploadFile returns the actual object name stored in MinIO (ppt/UUID.pptx)
            String objectName = fileStorageService.uploadFile(file, "ppt", title + ".pptx");
            return fileStorageService.getFileUrl(objectName);
        } catch (Exception e) {
            log.error("Failed to upload PPT: {}", e.getMessage());
            throw new RuntimeException("PPT上传失败: " + e.getMessage(), e);
        }
    }

    private TeachingMaterial saveTeachingMaterial(Course course, String fileUrl, Long fileSize) {
        TeachingMaterial material = new TeachingMaterial();
        material.setCourseId(course.getId());
        material.setKnowledgePointId(course.getKnowledgePointId());
        material.setTenantId(course.getTenantId());
        material.setMaterialType("ppt");
        material.setTitle(course.getTitle());
        material.setFileUrl(fileUrl);
        material.setFileSize(fileSize);
        material.setStatus("generated");
        material.setCreatedAt(java.time.LocalDateTime.now());

        teachingMaterialMapper.insert(material);
        return material;
    }

    /**
     * Extract narrativeArc from raw AI JSON content.
     * AI returns: "narrativeArc": {"opening": "...", "exploration": "...", "discovery": "...", "application": "..."}
     * We parse from raw JSON and set flattened fields on aiConfig.
     */
    private void extractNarrativeArc(String rawContent, PptGenerateRequest aiConfig) {
        try {
            // Extract the narrativeArc object using simple substring search
            // This avoids adding another Jackson model class for a one-time parse
            int arcStart = rawContent.indexOf("\"narrativeArc\"");
            if (arcStart < 0) return;

            int braceStart = rawContent.indexOf("{", arcStart);
            int braceEnd = rawContent.lastIndexOf("}");
            if (braceStart < 0 || braceEnd <= braceStart) return;

            String arcJson = rawContent.substring(braceStart + 1, braceEnd);
            aiConfig.setNarrativeArcOpening(extractJsonField(arcJson, "opening"));
            aiConfig.setNarrativeArcExploration(extractJsonField(arcJson, "exploration"));
            aiConfig.setNarrativeArcDiscovery(extractJsonField(arcJson, "discovery"));
            aiConfig.setNarrativeArcApplication(extractJsonField(arcJson, "application"));
        } catch (Exception e) {
            log.warn("Failed to extract narrativeArc: {}", e.getMessage());
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int valueStart = colon + 1;
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;
        char startChar = json.charAt(valueStart);
        int valueEnd;
        if (startChar == '"') {
            // String value — find closing quote (accounting for escaped quotes)
            valueEnd = valueStart + 1;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '\\' && valueEnd + 1 < json.length()) {
                    valueEnd += 2;
                    continue;
                }
                if (c == '"') {
                    valueEnd++;
                    break;
                }
                valueEnd++;
            }
            return json.substring(valueStart + 1, valueEnd - 1);
        } else if (startChar == '[') {
            // Array — find matching ]
            int depth = 1;
            valueEnd = valueStart + 1;
            while (valueEnd < json.length() && depth > 0) {
                char c = json.charAt(valueEnd);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        } else {
            // Number or boolean
            valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (Character.isLetterOrDigit(c) || c == '-' || c == '.' || c == ' ') {
                    valueEnd++;
                } else {
                    break;
                }
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    /**
     * Extract learningObjectives array from raw AI JSON.
     * AI returns: "learningObjectives": ["目标1", "目标2", ...]
     */
    private void extractLearningObjectives(String rawContent, PptGenerateRequest aiConfig) {
        try {
            int idx = rawContent.indexOf("\"learningObjectives\"");
            if (idx < 0) return;

            int bracketStart = rawContent.indexOf("[", idx);
            int bracketEnd = rawContent.lastIndexOf("]");
            if (bracketStart < 0 || bracketEnd <= bracketStart) return;

            String arrJson = rawContent.substring(bracketStart, bracketEnd + 1);
            // Parse individual string items from the array
            List<String> objectives = new ArrayList<>();
            int pos = 0;
            while (pos < arrJson.length()) {
                // Find next string value
                int q1 = arrJson.indexOf("\"", pos);
                if (q1 < 0) break;
                int q2 = q1 + 1;
                while (q2 < arrJson.length()) {
                    char c = arrJson.charAt(q2);
                    if (c == '\\' && q2 + 1 < arrJson.length()) {
                        q2 += 2;
                        continue;
                    }
                    if (c == '"') break;
                    q2++;
                }
                if (q2 > q1 + 1) {
                    objectives.add(arrJson.substring(q1 + 1, q2));
                }
                pos = q2 + 1;
                // Skip comma
                while (pos < arrJson.length() && (arrJson.charAt(pos) == ',' || arrJson.charAt(pos) == ' ')) {
                    pos++;
                }
            }
            if (!objectives.isEmpty()) {
                aiConfig.setLearningObjectives(objectives);
            }
        } catch (Exception e) {
            log.warn("Failed to extract learningObjectives: {}", e.getMessage());
        }
    }

    /**
     * Detect subject type (science=理科 | humanities=文科) from title and description.
     */
    private String detectSubjectType(String title, String description) {
        String text = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();
        // 理科 keywords
        String[] scienceKeywords = {"数学", "物理", "化学", "生物", "函数", "方程", "代数", "几何", "力学", "电磁", "光学", "原子", "细胞", "基因", "代谢", "欧姆", "牛顿", "酸碱", "溶解", "反应", "equation", "function", "physics", "chemistry", "biology", "math"};
        for (String kw : scienceKeywords) {
            if (text.contains(kw)) return "science";
        }
        return "humanities";
    }

    /**
     * Attempt to fix truncated JSON that contains a restarted generation.
     * When AI hits token limit mid-stream, it sometimes restarts from "slides": [ 
     * resulting in {"slides": [..., {"slides": [actual content...
     * This method finds the last "slides": [ and tries to build a valid JSON from it.
     */
    private String tryFixTruncatedJson(String raw) {
        try {
            // Find the last occurrence of "slides": [
            int lastSlidesIdx = raw.lastIndexOf("\"slides\": [");
            if (lastSlidesIdx < 0) return null;
            
            // Find the opening bracket after "slides":
            int bracketStart = raw.indexOf("[", lastSlidesIdx);
            if (bracketStart < 0) return null;
            
            // Find matching closing bracket by counting nesting depth
            int depth = 0;
            int i = bracketStart;
            boolean inString = false;
            while (i < raw.length()) {
                char c = raw.charAt(i);
                if (c == '"' && (i == 0 || raw.charAt(i-1) != '\\')) {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            // Found the matching bracket
                            String slidesContent = raw.substring(bracketStart, i + 1);
                            // Check if this looks like a real slides array (not just empty [])
                            if (slidesContent.length() < 20) return null;
                            
                            // Reconstruct: extract title/subtitle/narrativeArc from the first part,
                            // and use the slides from the second (complete) array
                            String prefix = raw.substring(0, lastSlidesIdx);
                            // Find title
                            String title = extractJsonField(prefix, "title");
                            String subtitle = extractJsonField(prefix, "subtitle");
                            
                            // Build reconstructed JSON
                            StringBuilder sb = new StringBuilder();
                            sb.append("{");
                            if (title != null) sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
                            if (subtitle != null) sb.append("\"subtitle\":\"").append(escapeJson(subtitle)).append("\",");
                            sb.append("\"slides\":").append(slidesContent);
                            sb.append("}");
                            return sb.toString();
                        }
                    }
                }
                i++;
            }
        } catch (Exception e) {
            log.debug("tryFixTruncatedJson failed: {}", e.getMessage());
        }
        return null;
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
