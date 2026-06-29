package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.model.ProviderType;
import com.aiteacher.provider.ai.model.UsageInfo;

import java.util.*;

/**
 * Mock LLM Provider for development and demonstration.
 * Returns contextually appropriate fake responses based on prompt content.
 * Enabled by setting ai.mock.enabled=true in application.yml.
 */
public class MockLLMProvider extends AbstractLLMProvider {

    private static final Random random = new Random();

    public MockLLMProvider() {
        this.enabled = true;
        this.priority = 1; // Low priority - used only when no real provider available
        this.model = "mock";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.MOCK;
    }

    @Override
    protected ChatResponse doChat(ChatRequest request) {
        String content = extractContent(request);
        String response = generateResponse(content);

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setId("mock-" + System.currentTimeMillis());
        chatResponse.setObject("chat.completion");
        chatResponse.setCreated(System.currentTimeMillis() / 1000);
        chatResponse.setModel(this.model);

        ChatChoice choice = new ChatChoice();
        choice.setIndex(0);
        choice.setFinishReason("stop");

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(response);
        choice.setMessage(assistantMsg);

        chatResponse.setChoices(new ChatChoice[]{choice});

        UsageInfo usage = UsageInfo.builder()
                .promptTokens(estimateTokens(content))
                .completionTokens(estimateTokens(response))
                .totalTokens(estimateTokens(content) + estimateTokens(response))
                .build();
        chatResponse.setUsage(usage);

        return chatResponse;
    }

    @Override
    protected List<String> doStream(ChatRequest request) {
        return Collections.emptyList();
    }

    private String extractContent(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : request.getMessages()) {
            if (msg.getContent() != null) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    private String generateResponse(String prompt) {
        String lower = prompt.toLowerCase();

        // Quiz/exam generation — check FIRST (more specific)
        // PPT slide content generation (JSON format) — check FIRST to take precedence over quiz keyword
        if ((lower.contains("幻灯片") || lower.contains("slide")) && lower.contains("json")) {
            return generateSlidesResponse(prompt);
        }

        // Quiz / exercise generation
        if (lower.contains("测验") || lower.contains("quiz") || lower.contains("题目") || lower.contains("试题")) {
            return generateQuizResponse(prompt);
        }

        // Course generation — check AFTER quiz to avoid "课程标题" prefix triggering course route
        if (lower.contains("课程") || lower.contains("course")) {
            return generateCourseResponse(prompt);
        }

        // Teaching material / PPT
        if (lower.contains("教材") || lower.contains("课件") || lower.contains("ppt")) {
            return generateMaterialResponse(prompt);
        }

        // Knowledge point
        if (lower.contains("知识点") || lower.contains("知识")) {
            return generateKnowledgeResponse(prompt);
        }

        // Video script
        if (lower.contains("视频") || lower.contains("脚本") || lower.contains("video")) {
            return generateVideoScriptResponse(prompt);
        }

        // Default conversational response
        return generateDefaultResponse(prompt);
    }

    private String generateSlidesResponse(String prompt) {
        // DEBUG: log the prompt so we can trace which provider is actually being called
        System.err.println("##### MockLLMProvider.generateSlidesResponse CALLED #####");
        System.err.println("##### prompt length: " + (prompt == null ? "NULL" : prompt.length()));
        System.err.println("##### prompt class: " + (prompt == null ? "NULL" : prompt.getClass().getName()));
        System.out.flush();
        System.err.println("Prompt (first 500 chars): " + (prompt == null ? "NULL" : prompt.substring(0, Math.min(500, prompt.length()))));
        System.out.flush();
        System.err.flush();

        // ── 1. Detect subject type from prompt ─────────────────────────────────
        String lower = prompt.toLowerCase();
        boolean isScienceFromPrompt =
            !lower.contains("历史") && !lower.contains("语文") && !lower.contains("政治")
            && !lower.contains("地理") && !lower.contains("英语") && !lower.contains("美术")
            && !lower.contains("音乐") && !lower.contains("思想")
            && (lower.contains("数学") || lower.contains("物理") || lower.contains("化学")
                || lower.contains("生物") || lower.contains("科学") || lower.contains("函数")
                || lower.contains("方程") || lower.contains("力学") || lower.contains("电磁"));

        // ── 2. Extract real course title from prompt ──────────────────────────────
        // Prompt format contains "标题：%s" where %s is the actual course title
        String courseTitle = extractCourseTitle(prompt);
        String safeTitle = courseTitle != null && !courseTitle.isBlank() ? courseTitle.trim() : "未命名课程";

        // ── 3. Re-detect subject type after title extraction ─────────────────────
        // Title may contain stronger signals (e.g. "二次函数" contains science keyword)
        String titleLower = safeTitle.toLowerCase();
        boolean isScienceFromTitle =
            titleLower.contains("数学") || titleLower.contains("物理") || titleLower.contains("化学")
            || titleLower.contains("生物") || titleLower.contains("科学") || titleLower.contains("函数")
            || titleLower.contains("方程") || titleLower.contains("力学") || titleLower.contains("电磁")
            || titleLower.contains("几何") || titleLower.contains("代数") || titleLower.contains("微积");

        // Combine: explicit humanities keywords override; otherwise fall back to prompt + title detection
        boolean isExplicitHumanities = lower.contains("历史") || lower.contains("语文") || lower.contains("政治")
            || lower.contains("地理") || lower.contains("英语") || lower.contains("美术")
            || lower.contains("音乐") || lower.contains("哲学");

        // Priority fix: if the prompt explicitly declares "学科类型：science", respect that declaration
        // even if "文科必须包含..." appears in the slide type requirements (examples section).
        // We check for the explicit declaration BEFORE the humanities override takes effect.
        boolean hasExplicitScienceDeclaration = lower.contains("学科类型：science") || lower.contains("学科类型：science（");
        boolean hasExplicitHumanitiesDeclaration = lower.contains("学科类型：humanities");

        System.err.println("##### isScience check: lower=" + lower.substring(0, Math.min(200, lower.length())));
        System.err.println("##### isExplicitHumanities=" + isExplicitHumanities + " isScienceFromPrompt=" + isScienceFromPrompt + " isScienceFromTitle=" + isScienceFromTitle);
        System.err.println("##### hasExplicitScienceDeclaration=" + hasExplicitScienceDeclaration + " hasExplicitHumanitiesDeclaration=" + hasExplicitHumanitiesDeclaration);
        boolean isScience;
        if (hasExplicitScienceDeclaration) {
            // Explicit declaration wins — respect "学科类型：science"
            isScience = true;
        } else if (hasExplicitHumanitiesDeclaration || isExplicitHumanities) {
            // Explicit humanities declaration or unambiguous humanities keywords
            isScience = false;
        } else {
            isScience = isScienceFromTitle || isScienceFromPrompt;
        }

        // ── 4. Generate subject-appropriate slides ────────────────────────────────
        if (isScience) {
            return generateScienceSlides(safeTitle);
        } else {
            return generateHumanitiesSlides(safeTitle);
        }
    }

    /**
     * Extract the course title from the PPT generation prompt.
     * The prompt contains "标题：{courseTitle}" after the format placeholders.
     */
    private String extractCourseTitle(String prompt) {
        // Look for "标题：" followed by the actual title (usually before the next newline or description marker)
        int idx = prompt.indexOf("标题：");
        if (idx < 0) idx = prompt.indexOf("标题：");
        if (idx < 0) return null;
        int start = idx + "标题：".length();
        // Title ends at newline or certain delimiters
        int end = prompt.length();
        for (int i = start; i < prompt.length(); i++) {
            char c = prompt.charAt(i);
            if (c == '\n' || c == '\r' || (i + 2 < prompt.length() && (prompt.substring(i, i + 2).equals("描述") || prompt.substring(i, i + 2).equals("章节")))) {
                end = i;
                break;
            }
        }
        String title = prompt.substring(start, end).trim();
        // Remove surrounding quotes if any
        if (title.startsWith("\"") && title.endsWith("\"")) {
            title = title.substring(1, title.length() - 1);
        }
        return title.length() > 0 ? title : null;
    }

    // ── 课程知识库（Course Knowledge Base）──────────────────────────────────────
    // key → 匹配关键词，value → 该课程的真实教学内容
    private static final Map<String, CourseKnowledge> COURSE_KNOWLEDGE = new LinkedHashMap<>();

    static {
        // ── 课程1: 二次函数的基本概念 ───────────────────────────────────────
        COURSE_KNOWLEDGE.put("二次函数", new CourseKnowledge(
            "二次函数的基本概念",
            "y = ax² + bx + c（a ≠ 0）",
            new String[]{
                "一般式：y = ax² + bx + c（a ≠ 0）",
                "顶点式：y = a(x-h)² + k，顶点坐标为 (h, k)",
                "交点式：y = a(x-x₁)(x-x₂)，与 x 轴交于 (x₁,0) 和 (x₂,0)"
            },
            // problem slide
            "小明家门前有一座拱桥，桥拱最高点离水面3米，桥面宽8米。汽车从桥下经过时，高度随水平位置变化的规律是什么？能否用数学函数描述？",
            // concept bullets
            new String[]{
                "二次函数的定义：形如 y = ax² + bx + c（a ≠ 0）的函数叫二次函数",
                "二次项系数 a 决定开口方向：a > 0 开口向上，a < 0 开口向下",
                "顶点坐标 (-b/2a, (4ac-b²)/4a)，对称轴为 x = -b/2a"
            },
            // derivation steps
            new String[]{
                "第一步：写出一般式 y = ax² + bx + c（a ≠ 0）",
                "第二步：利用配方法，将一般式化为顶点式 y = a(x + b/2a)² + (4ac-b²)/4a",
                "第三步：令 h = -b/2a，k = (4ac-b²)/4a，得到顶点式 y = a(x-h)² + k"
            },
            // exercise problem
            "【典型例题】已知二次函数 y = x² - 4x + 3，\n（1）求顶点坐标和对称轴；\n（2）求与 x 轴的交点坐标；\n（3）求函数的最小值。",
            // exercise solution
            "解答步骤：\nStep 1：配方 y = (x-2)² - 1，对称轴 x = 2，顶点 (2, -1)\nStep 2：令 y = 0，得 (x-2)² = 1，x-2 = ±1，x = 3 或 x = 1\nStep 3：当 x = 2 时，y_min = -1",
            // infographic bullets
            new String[]{
                "核心公式：y = ax² + bx + c（a ≠ 0），顶点 (-b/2a, (4ac-b²)/4a)",
                "图像特征：开口方向由 a 决定，顶点是最低/最高点，对称轴 x = -b/2a",
                "实际应用：抛物线运动（篮球/喷泉）、拱桥设计、优化问题（利润最大/成本最小）"
            },
            // quiz questions
            new String[][]{
                {"下列函数中，哪些是二次函数？", "A. y = x²  B. y = 2x+1  C. y = (x+1)(x-2)  D. y = 1/x²", "A,C", "A和C展开后都是 ax²+bx+c（a≠0）的形式，B是一次函数，D是反比例函数"},
                {"二次函数 y = -x² + 4x + 1 的顶点坐标是？", "A. (2,5)  B. (2,3)  C. (-2,5)  D. (4,1)", "A", "顶点 h = -b/2a = -4/-2 = 2，k = (4ac-b²)/4a = (4·(-1)·1-16)/-4 = 20/4 = 5，故顶点为 (2,5)"},
                {"a > 0 时，二次函数图像开口方向是？", "A. 向上  B. 向下  C. 向左  D. 向右", "A", "a > 0 开口向上，a < 0 开口向下"}
            },
            // vote question
            "二次函数 y = (x-1)² 的图像，可以由 y = x² 的图像怎样变换得到？",
            new String[]{"A. 向左平移1个单位", "B. 向右平移1个单位", "C. 向上平移1个单位", "D. 向下平移1个单位"},
            // exit ticket question
            "下课前请回答：二次函数 y = 2x² - 8x + 5 的顶点坐标是什么？对称轴是哪条直线？"
        ));

        // ── 课程2: 电磁感应原理 ──────────────────────────────────────────────
        COURSE_KNOWLEDGE.put("电磁感应", new CourseKnowledge(
            "电磁感应原理",
            "E = nΔΦ/Δt（法拉第定律）",
            new String[]{
                "法拉第电磁感应定律：感应电动势 E = n|ΔΦ/Δt|，Φ = BS cosθ",
                "楞次定律：感应电流的磁场总是阻碍原磁场的变化",
                "E = BLv sinθ（导体切割磁感线时的感应电动势）"
            },
            // problem slide
            "如图所示，在水平方向的匀强磁场中，一根长度为 0.5m 的金属棒以 10m/s 的速度垂直切割磁感线。磁感应强度 B = 0.2T。请问：金属棒两端产生的感应电动势是多大？",
            // concept bullets
            new String[]{
                "磁通量 Φ = BS cosθ：穿过某一面积的磁感线条数，θ 为面积法线与磁场方向的夹角",
                "电磁感应现象：穿过闭合回路的磁通量发生变化时，回路中产生感应电流的现象",
                "感应电动势的大小与磁通量变化率成正比，与具体如何变化无关"
            },
            // derivation steps
            new String[]{
                "第一步：写出磁通量定义 Φ = BS cosθ，当线圈平面与磁场垂直时 Φ = BS",
                "第二步：法拉第电磁感应定律 E = n|ΔΦ/Δt|，n 为线圈匝数",
                "第三步：对于导体切割磁感线情况，有 E = BLv sinθ（B⊥v 时 sinθ = 1，E = BLv）"
            },
            // exercise problem
            "【典型例题】一矩形线圈共 100 匝，面积为 0.01m²，在 0.1s 内磁感应强度从 0.2T 增加到 0.8T（磁场方向始终垂直于线圈平面）。求：（1）磁通量的变化量；（2）感应电动势的大小。",
            // exercise solution
            "解答步骤：\nStep 1：ΔΦ = Φ₂ - Φ₁ = BS₂ - BS₁ = B₂S - B₁S = (0.8-0.2)×0.01 = 0.006Wb\nStep 2：E = nΔΦ/Δt = 100 × 0.006/0.1 = 6V",
            // infographic bullets
            new String[]{
                "核心公式：E = n|ΔΦ/Δt|（法拉第定律），E = BLv sinθ（切割条件）",
                "楞次定律：感应电流的磁场方向总是阻碍原磁场的变化（增反减同）",
                "实际应用：发电机（机械能→电能）、变压器、电磁炉、磁带录音原理"
            },
            // quiz questions
            new String[][]{
                {"根据法拉第电磁感应定律，感应电动势与什么成正比？", "A. 磁通量  B. 磁通量变化量  C. 磁通量变化率  D. 磁感应强度", "C", "E = n|ΔΦ/Δt|，与磁通量变化率成正比，与磁通量大小无关"},
                {"导体棒在匀强磁场中垂直切割磁感线时，感应电动势的表达式为？", "A. E = BLv  B. E = B/L·v  C. E = L/B·v  D. E = B·L/v", "A", "当导体垂直切割磁感线时，E = BLv，v 为切割速度"},
                {"使用楞次定律判断感应电流方向时，以下正确的是？", "A. 磁通量增加时，感应电流磁场方向与原磁场方向相同", "B. 磁通量减少时，感应电流磁场方向与原磁场方向相反", "C. 感应电流的磁场总是阻碍原磁场的变化", "D. 感应电流磁场方向与原磁场方向始终相反", "C", "楞次定律的核心：感应电流的磁场总是阻碍原磁场的变化，即增反减同"}
            },
            // vote question
            "当穿过线圈的磁通量突然增加时，感应电流产生的磁场方向是？",
            new String[]{"A. 与原磁场方向相同", "B. 与原磁场方向相反", "C. 垂直于原磁场方向", "D. 先相同后相反"},
            // exit ticket question
            "下课前请回答：一个线圈共有 50 匝，在 0.2s 内磁通量从 0 增加到 0.04Wb，产生的感应电动势是多大？"
        ));
    }

    /**
     * 课程知识数据结构
     */
    private static class CourseKnowledge {
        String title;
        String coreFormula;
        String[] expressions;         // 三种表达式
        String problemScenario;        // 生活情境题
        String[] conceptBullets;       // 核心概念要点
        String[] derivationSteps;      // 推导步骤
        String exerciseProblem;        // 例题
        String exerciseSolution;       // 例题解答
        String[] infographicBullets;  // 知识结构图要点
        String[][] quizQuestions;      // 测验题 [question, options, answer, explanation]
        String voteQuestion;           // 投票问题
        String[] voteOptions;          // 投票选项
        String exitTicketQuestion;     // 出门票问题

        CourseKnowledge(String title, String coreFormula, String[] expressions,
                String problemScenario, String[] conceptBullets, String[] derivationSteps,
                String exerciseProblem, String exerciseSolution, String[] infographicBullets,
                String[][] quizQuestions, String voteQuestion, String[] voteOptions,
                String exitTicketQuestion) {
            this.title = title;
            this.coreFormula = coreFormula;
            this.expressions = expressions;
            this.problemScenario = problemScenario;
            this.conceptBullets = conceptBullets;
            this.derivationSteps = derivationSteps;
            this.exerciseProblem = exerciseProblem;
            this.exerciseSolution = exerciseSolution;
            this.infographicBullets = infographicBullets;
            this.quizQuestions = quizQuestions;
            this.voteQuestion = voteQuestion;
            this.voteOptions = voteOptions;
            this.exitTicketQuestion = exitTicketQuestion;
        }
    }

    // ── Science (理科) slide set ────────────────────────────────────────────────
    // 强调：准确性 × 生动性 × 互动性
    // 顺序：chapter → title → problem → concept → derivation → exercise → infographic → comic → quiz → vote → summary → exit-ticket
    private String generateScienceSlides(String courseTitle) {
        // 根据课程标题匹配知识库
        CourseKnowledge knowledge = findKnowledge(courseTitle);

        return String.format("""
                {
                  "title": "%s",
                  "subtitle": "原理 · 推导 · 应用",
                  "narrativeArc": {
                    "opening": "从生活中的真实问题引入，激发探索物理规律的兴趣",
                    "exploration": "通过实验现象和数学推导，逐步揭示概念与公式",
                    "discovery": "归纳出一般规律，完成公式推导",
                    "application": "将知识应用于解释生活现象和解决实际问题"
                  },
                  "learningObjectives": [
                    "理解%s的核心概念和基本原理",
                    "能够运用公式解决典型问题",
                    "将理论知识与生活应用相结合"
                  ],
                  "slides": [
                    {
                      "slideType": "chapter",
                      "title": "本章学习目标",
                      "mainBody": "理解基本概念，掌握核心公式，能够灵活应用",
                      "visualGuidance": "大字居中，左侧列出目标，右侧配图",
                      "layout": "full-bleed",
                      "artStyle": "hand-drawn-edu",
                      "duration": 2,
                      "teacherNote": "带领学生快速浏览本章要掌握的知识点"
                    },
                    {
                      "slideType": "title",
                      "title": "%s",
                      "subtitle": "原理·推导·应用",
                      "mainBody": "从生活中发现问题，用科学方法分析问题，最终解决问题",
                      "visualGuidance": "简洁大气，公式感强，配合科学家卡通形象",
                      "layout": "full-bleed",
                      "artStyle": "hand-drawn-edu",
                      "duration": 2,
                      "teacherNote": "从生活中的真实问题切入，激发学生兴趣"
                    },
                    {
                      "slideType": "problem",
                      "title": "生活中的现象",
                      "mainBody": "观察以下现象，思考背后的原理是什么？",
                      "scenario": "%s",
                      "visualGuidance": "四格漫画展示生活中常见现象，右侧留白供学生思考",
                      "layout": "bento",
                      "artStyle": "hand-drawn-edu",
                      "duration": 3,
                      "teacherNote": "让学生先观察现象，再讨论原因，引出新知识"
                    },
                    {
                      "slideType": "concept",
                      "title": "核心概念",
                      "mainBody": "%s",
                      "bullets": ["%s", "%s", "%s"],
                      "formula": "%s",
                      "visualGuidance": "左侧大字定义和公式，右侧配图或示意图",
                      "layout": "two-col",
                      "artStyle": "hand-drawn-edu",
                      "duration": 4,
                      "teacherNote": "慢讲，细讲，举生活中的反例帮助理解"
                    },
                    {
                      "slideType": "derivation",
                      "title": "公式推导",
                      "mainBody": "一步步推导，从已知走向未知",
                      "steps": ["%s", "%s", "%s"],
                      "conclusion": "最终结论：所得公式适用于所有满足条件的情况",
                      "visualGuidance": "推导过程分步展示，每一步用箭头连接，配合颜色标注关键变形",
                      "layout": "linear-progression",
                      "artStyle": "hand-drawn-edu",
                      "duration": 5,
                      "teacherNote": "边讲边写，带着学生一步步演算，不要跳步"
                    },
                    {
                      "slideType": "exercise",
                      "title": "例题演练",
                      "exampleProblem": "%s",
                      "solution": "%s",
                      "variants": ["变式1：改变条件，重新求解", "变式2：逆向提问，检验理解", "变式3：联系实际，应用拓展"],
                      "visualGuidance": "左侧例题+分步解答，右侧变式题区，配图说明",
                      "layout": "two-col",
                      "artStyle": "hand-drawn-edu",
                      "duration": 6,
                      "teacherNote": "先让学生独立思考，再逐步讲解，最后做变式训练"
                    },
                    {
                      "slideType": "infographic",
                      "title": "知识结构图",
                      "mainBody": "本章知识脉络一览",
                      "bullets": ["%s", "%s", "%s"],
                      "visualGuidance": "中心辐射图（Hub-spoke），中心是核心公式，四周是衍生概念和应用",
                      "layout": "hub-spoke",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "用思维导图串联知识点，帮助学生形成结构化认知"
                    },
                    {
                      "slideType": "comic",
                      "title": "科学家的故事",
                      "comicPanels": [
                        { "scene": "实验室里，科学家被一个自然现象吸引", "dialogue": "科学家：这个现象背后一定有什么规律……", "caption": "发现问题" },
                        { "scene": "科学家翻阅资料，反复思考，提出猜想", "dialogue": "科学家：也许和某个物理量有关……", "caption": "提出假设" },
                        { "scene": "科学家设计实验验证，终于发现规律", "dialogue": "科学家：我验证了！这就是定律！", "caption": "验证结论" }
                      ],
                      "visualGuidance": "3格漫画，手绘科学家人物形象，对话气泡清晰",
                      "layout": "grid",
                      "artStyle": "hand-drawn-edu",
                      "duration": 3,
                      "teacherNote": "讲完知识点后，用漫画展示科学发现的完整过程"
                    },
                    {
                      "slideType": "quiz",
                      "title": "随堂测验",
                      "quizQuestions": [
                        { "type": "choice", "question": "%s", "options": %s, "answer": "%s", "explanation": "%s" },
                        { "type": "choice", "question": "%s", "options": %s, "answer": "%s", "explanation": "%s" }
                      ],
                      "visualGuidance": "题目大字居中，选项用彩色卡片展示",
                      "layout": "full-bleed",
                      "artStyle": "hand-drawn-edu",
                      "duration": 4,
                      "teacherNote": "限时作答，作答后立即公布答案并简要讲解"
                    },
                    {
                      "slideType": "vote",
                      "title": "课堂投票",
                      "question": "%s",
                      "options": %s,
                      "visualGuidance": "大字问题，多个选项卡片并列，风格活泼，支持匿名投票",
                      "layout": "binary-comparison",
                      "artStyle": "corporate-memphis",
                      "duration": 2,
                      "teacherNote": "发起投票，展示实时柱状图，引发学生讨论"
                    },
                    {
                      "slideType": "summary",
                      "title": "本节小结",
                      "mainBody": "本节核心：掌握基本概念，理解公式推导，熟练应用于实际问题",
                      "bullets": ["核心概念：%s", "关键公式：%s", "易错点：公式适用条件和单位"],
                      "visualGuidance": "左侧核心要点列表，右侧配知识结构图，用颜色区分重点",
                      "layout": "two-col",
                      "artStyle": "hand-drawn-edu",
                      "duration": 3,
                      "teacherNote": "用思维导图快速回顾，标注学生易错点"
                    },
                    {
                      "slideType": "exit-ticket",
                      "title": "出门票",
                      "question": "%s",
                      "visualGuidance": "问题大字居中，下方留出手写区域，风格简洁",
                      "layout": "full-bleed",
                      "artStyle": "hand-drawn-edu",
                      "duration": 2,
                      "teacherNote": "学生写在纸上，下课时收齐，了解学生对核心知识的掌握情况"
                    }
                  ]
                }
                """,
            // === 模板变量（按顺序对应 %s）— 所有字符串都经过 JSON 转义 ===
            jsonEscape(knowledge.title),                                    // 1  title
            jsonEscape(knowledge.title),                                    // 2  learningObjectives subtitle
            jsonEscape(knowledge.title),                                    // 3  title slide subtitle
            jsonEscape(knowledge.problemScenario),                          // 4  problem scenario
            jsonEscape(knowledge.conceptBullets[0]),                        // 5  concept mainBody (first bullet)
            jsonEscape(knowledge.conceptBullets[0]), jsonEscape(knowledge.conceptBullets[1]), jsonEscape(knowledge.conceptBullets[2]), // 6-8 bullets
            jsonEscape(knowledge.coreFormula),                              // 9  formula
            jsonEscape(knowledge.derivationSteps[0]), jsonEscape(knowledge.derivationSteps[1]), jsonEscape(knowledge.derivationSteps[2]), // 10-12 steps
            jsonEscape(knowledge.exerciseProblem),                          // 13 exercise problem
            jsonEscape(knowledge.exerciseSolution),                        // 14 exercise solution
            jsonEscape(knowledge.infographicBullets[0]), jsonEscape(knowledge.infographicBullets[1]), jsonEscape(knowledge.infographicBullets[2]), // 15-17 infographic
            // quiz 1
            jsonEscape(knowledge.quizQuestions[0][0]), optionsArray(knowledge.quizQuestions[0][1]), knowledge.quizQuestions[0][2], jsonEscape(knowledge.quizQuestions[0][3]),
            // quiz 2
            jsonEscape(knowledge.quizQuestions[1][0]), optionsArray(knowledge.quizQuestions[1][1]), knowledge.quizQuestions[1][2], jsonEscape(knowledge.quizQuestions[1][3]),
            // vote
            jsonEscape(knowledge.voteQuestion), optionsArrayStr(knowledge.voteOptions),
            // summary
            jsonEscape(knowledge.conceptBullets[0]), jsonEscape(knowledge.coreFormula),
            // exit ticket
            jsonEscape(knowledge.exitTicketQuestion)
        );
    }

    /** JSON 字符串转义 — 处理换行、引号等特殊字符 */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    /** 根据课程标题匹配知识库 */
    private CourseKnowledge findKnowledge(String courseTitle) {
        if (courseTitle == null) return getGenericKnowledge();
        for (Map.Entry<String, CourseKnowledge> entry : COURSE_KNOWLEDGE.entrySet()) {
            if (courseTitle.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return getGenericKnowledge();
    }

    /** 通用的理科内容（知识库未命中时使用） */
    private CourseKnowledge getGenericKnowledge() {
        return new CourseKnowledge(
            "理科课程",
            "公式",
            new String[]{"表达式一", "表达式二", "表达式三"},
            "小明骑车时发现：下坡时不蹬车，车越来越快；上坡时使劲蹬，车也越来越慢。这说明什么？",
            new String[]{"概念一：理解基本定义", "概念二：掌握核心原理", "概念三：区分易错点"},
            new String[]{"第一步：写出已知条件", "第二步：运用原理进行计算", "第三步：化简得到最终结果"},
            "【典型例题】根据已知条件，求解问题……",
            "解答步骤：\nStep 1：分析条件\nStep 2：代入公式\nStep 3：计算结果",
            new String[]{"核心公式：清晰展示，标注符号", "推导路径：从基本原理到结论", "应用场景：列举3个以上实例"},
            new String[][]{
                {"下列说法正确的是？", "A. 选项一  B. 选项二  C. 选项三  D. 选项四", "B", "根据原理，正确答案是B，因为……"},
                {"关于公式的使用，下列说法正确的是？", "A. 适用所有情况  B. 只适用特定条件  C. 需要单位统一  D. 可任意推广", "B,C", "公式有适用条件，使用时需注意单位统一"}
            },
            "这道题，选A还是选B？",
            new String[]{"A. 选项一", "B. 选项二", "C. 选项三", "D. 选项四"},
            "下课前请回答：今天学的核心公式是什么？它的适用条件是什么？"
        );
    }

    /** 将选项字符串转为 JSON 数组字符串 */
    private String optionsArray(String optionsStr) {
        String[] opts = optionsStr.split("\\s+(?=[A-Z])");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < opts.length; i++) {
            sb.append("\"").append(opts[i].trim()).append("\"");
            if (i < opts.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /** 将选项数组转为 JSON 数组字符串 */
    private String optionsArrayStr(String[] opts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < opts.length; i++) {
            sb.append("\"").append(opts[i]).append("\"");
            if (i < opts.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Humanities (文科) slide set ──────────────────────────────────────────────
    // 强调：趣味性 × 生动性 × 故事性
    // 顺序：title(封面) → chapter(章节) → case(案例) → timeline(时间线) → comic(漫画) → quote(名言) → reflection(思考) → infographic(信息图) → vote(投票) → quiz(测验) → activity(活动) → summary(小结) → exit-ticket(出门票)
    private String generateHumanitiesSlides(String courseTitle) {
        return String.format("""
                {
                  "title": "%s",
                  "subtitle": "故事 · 思考 · 感悟",
                  "narrativeArc": {
                    "opening": "从一个引人入胜的故事/现象引入，激发学生好奇心",
                    "exploration": "通过丰富的案例和故事层层展开，引发共情和思考",
                    "discovery": "在讨论和互动中发现规律，理解深层含义",
                    "application": "联系现实，引发学生对人生/社会的反思"
                  },
                  "learningObjectives": [
                    "了解核心知识点的背景和来龙去脉",
                    "能够结合案例进行分析和表达",
                    "形成自己的观点和思考"
                  ],
                  "slides": [
                    {
                      "slideType": "chapter",
                      "title": "本章导学",
                      "mainBody": "带着问题进入本章：为什么会这样？有什么启示？",
                      "visualGuidance": "章节标题页，大字居中，背景配相关历史场景图",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 2,
                      "teacherNote": "先用问题引发好奇，再进入正题"
                    },
                    {
                      "slideType": "title",
                      "title": "%s",
                      "subtitle": "故事 · 思考 · 感悟",
                      "mainBody": "一段历史，一个故事，一种思想，一种力量",
                      "visualGuidance": "古风/典雅设计，配合主题元素（水墨/建筑/人物剪影）",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 2,
                      "teacherNote": "营造氛围，让学生快速进入学习情境"
                    },
                    {
                      "slideType": "case",
                      "title": "故事引入",
                      "mainBody": "这是一个关于……的真实故事",
                      "scenario": "时间回到……，地点……，人物……经历了一件什么事？这件事为什么重要？",
                      "visualGuidance": "故事场景配图（古建筑/人物/地图），文字区在右侧或下方，风格叙事感强",
                      "layout": "two-col",
                      "artStyle": "corporate-memphis",
                      "duration": 4,
                      "teacherNote": "声情并茂地讲述故事，引导学生思考故事背后的意义"
                    },
                    {
                      "slideType": "timeline",
                      "title": "发展脉络",
                      "mainBody": "从古至今，这件事/这个人物经历了怎样的发展？",
                      "visualGuidance": "横向时间线图，关键节点用图标标注，连接线清晰，配简短说明",
                      "layout": "linear-progression",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "沿时间线梳理发展脉络，强调关键转折点"
                    },
                    {
                      "slideType": "comic",
                      "title": "情境再现",
                      "comicPanels": [
                        { "scene": "时间：……地点：……", "dialogue": "人物A：……", "caption": "背景介绍" },
                        { "scene": "事件发展……", "dialogue": "人物B：……", "caption": "高潮部分" },
                        { "scene": "结果/影响……", "dialogue": "旁白：……", "caption": "意义揭示" }
                      ],
                      "visualGuidance": "6格漫画，风格叙事性强，画面之间有因果关联，有留白供思考",
                      "layout": "grid",
                      "artStyle": "corporate-memphis",
                      "duration": 4,
                      "teacherNote": "让学生分角色扮演漫画中的人物，增强代入感"
                    },
                    {
                      "slideType": "quote",
                      "title": "名人名言",
                      "quoteText": "「……」",
                      "quoteAuthor": "—— 名人姓名（身份简介）",
                      "mainBody": "这句话体现了什么思想？对你有什么启发？",
                      "visualGuidance": "大号引号，居中排版，名言用特殊字体，背景淡雅，配人物肖像",
                      "layout": "full-bleed",
                      "artStyle": "vintage",
                      "duration": 3,
                      "teacherNote": "让学生先读名言，再分享自己的理解"
                    },
                    {
                      "slideType": "reflection",
                      "title": "思考与讨论",
                      "mainBody": "读了/学了这些内容，你有什么想法？",
                      "visualGuidance": "问题大字居中，背景留白感强，鼓励学生发散思维",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "给学生2-3分钟独立思考，然后小组讨论"
                    },
                    {
                      "slideType": "infographic",
                      "title": "知识全景图",
                      "mainBody": "本章知识全景一览",
                      "bullets": ["对比表格：横向对比多个概念的异同", "分类图：按某种标准将知识点分类", "关系图：展示不同知识点之间的联系"],
                      "visualGuidance": "Bento布局，每个格子一个知识点，配图标，颜色区分不同类别",
                      "layout": "dense-modules",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "用图表帮助学生建立结构化认知"
                    },
                    {
                      "slideType": "vote",
                      "title": "课堂投票",
                      "question": "你怎么看这个问题？",
                      "options": ["A. 观点一：……", "B. 观点二：……", "C. 观点三：……", "D. 其他想法：……"],
                      "visualGuidance": "问题居中大字，四个选项卡片横向排列，风格活泼，支持匿名投票",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "发起投票，展示实时结果，引导学生讨论为什么不同观点的人比例不同"
                    },
                    {
                      "slideType": "quiz",
                      "title": "知识巩固",
                      "quizQuestions": [
                        { "type": "choice", "question": "关于本章内容，下列说法正确的是？", "options": ["A. ", "B. ", "C. ", "D. "], "answer": "C", "explanation": "本题考察对核心概念的准确理解，正确答案是C，因为……" },
                        { "type": "choice", "question": "这个故事/人物给你的最大启示是什么？", "options": ["A. 启示一", "B. 启示二", "C. 启示三", "D. 其他"], "answer": "B", "explanation": "从故事/人物的经历中，我们可以看到……这给我们的启示是……" }
                      ],
                      "visualGuidance": "题目大字居中，选项用彩色卡片展示",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 4,
                      "teacherNote": "通过测验检验学习效果，及时纠正理解偏差"
                    },
                    {
                      "slideType": "activity",
                      "title": "小组讨论",
                      "activityDesc": "分组讨论：在我们的生活中，是否有类似的现象/问题？我们应该怎么做？\\n\\n要求：\\n1. 每组4-6人\\n2. 讨论时间5分钟\\n3. 每组推选一人总结发言",
                      "visualGuidance": "活动规则清晰展示，左侧文字说明，右侧配小组讨论插图",
                      "layout": "two-col",
                      "artStyle": "corporate-memphis",
                      "duration": 8,
                      "teacherNote": "巡视各组讨论情况，鼓励每个学生发言"
                    },
                    {
                      "slideType": "summary",
                      "title": "本节小结",
                      "mainBody": "今天我们学到了什么？",
                      "bullets": ["核心知识点：", "重要启示：", "联系现实："],
                      "visualGuidance": "左侧要点，右侧配主题插图，简洁大气",
                      "layout": "two-col",
                      "artStyle": "corporate-memphis",
                      "duration": 3,
                      "teacherNote": "快速回顾，联系开头的问题，确认达成学习目标"
                    },
                    {
                      "slideType": "exit-ticket",
                      "title": "出门票",
                      "question": "下课前，请写下：今天最让你印象深刻的一点是什么？有什么想进一步了解的？",
                      "visualGuidance": "问题居中，留白充足，风格简洁温暖",
                      "layout": "full-bleed",
                      "artStyle": "corporate-memphis",
                      "duration": 2,
                      "teacherNote": "学生写在纸上，了解学生对课堂的真实感受和疑问"
                    }
                  ]
                }
                """, courseTitle, courseTitle);
    }

    private String generateCourseResponse(String prompt) {
        return """
                {
                  "title": "电磁感应原理",
                  "description": "本课程系统讲解法拉第电磁感应定律和楞次定律，使学生理解电磁感应的基本原理及其应用。",
                  "totalDuration": 180,
                  "chapters": [
                    {
                      "title": "电磁感应基础",
                      "duration": 45,
                      "keyPoints": ["法拉第电磁感应定律", "感应电动势的定义", "磁通量的概念"],
                      "teachingNotes": "通过实验演示引入概念"
                    },
                    {
                      "title": "楞次定律",
                      "duration": 60,
                      "keyPoints": ["感应电流方向的判断", "增反减同原则", "能量守恒与电磁感应"],
                      "teachingNotes": "结合生活实例讲解"
                    },
                    {
                      "title": "电磁感应应用",
                      "duration": 75,
                      "keyPoints": ["发电机原理", "变压器原理", "电磁炉原理"],
                      "teachingNotes": "理论与实践相结合"
                    }
                  ]
                }
                """;
    }

    private String generateQuizResponse(String prompt) {
        // Detect difficulty and type from prompt
        String lower = prompt.toLowerCase();
        String difficulty = "medium";
        String type = "mixed";
        if (lower.contains("简单") || lower.contains("easy")) difficulty = "easy";
        else if (lower.contains("困难") || lower.contains("hard")) difficulty = "hard";
        if (lower.contains("选择题")) type = "choice";
        else if (lower.contains("填空题")) type = "blank";
        else if (lower.contains("简答题")) type = "essay";

        // Extract count from prompt (look for a number near "题" or "count")
        int count = 5;
        for (int i = 0; i < prompt.length() - 1; i++) {
            if (prompt.charAt(i) >= '0' && prompt.charAt(i) <= '9') {
                int j = i;
                while (j < prompt.length() && prompt.charAt(j) >= '0' && prompt.charAt(j) <= '9') j++;
                String num = prompt.substring(i, j);
                if (j < prompt.length() && (prompt.charAt(j) == '题' || prompt.substring(j).startsWith("道试题"))) {
                    try { count = Integer.parseInt(num); } catch (Exception ignored) {}
                    break;
                }
            }
        }
        count = Math.max(1, Math.min(count, 20));

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"questions\": [\n");

        // Generate choice questions
        int choiceCount = (type.equals("choice") || type.equals("mixed")) ? Math.max(1, count / 2) : 0;
        // Generate blank questions
        int blankCount = type.equals("blank") ? count : 0;
        // Generate essay questions
        int essayCount = type.equals("essay") ? count : 0;
        if (type.equals("mixed")) essayCount = count - choiceCount;

        String[] easyQuestions = {
            "人工智能的英文缩写是什么？", "机器学习属于人工智能的哪个分支？",
            "深度学习通常使用哪种网络结构？", "自然语言处理简称是？",
            "计算机视觉主要研究什么？", "监督学习需要什么数据？"
        };
        String[] mediumQuestions = {
            "下列哪个算法不属于监督学习？", "卷积神经网络主要用于哪类任务？",
            "Transformer架构的核心机制是？", "反向传播算法的作用是？",
            "下列哪个不是常见的激活函数？", "梯度消失问题通常发生在？"
        };
        String[] hardQuestions = {
            "请解释GAN网络中生成器和判别器的对抗机制。",
            "BERT模型的双向编码特性是如何实现的？",
            "深度学习中batch size对模型收敛有什么影响？",
            "解释注意力机制中Query、Key、Value的作用。",
            "ResNet通过什么方式解决了网络深度带来的退化问题？"
        };
        String[][] questionsByDiff = { easyQuestions, mediumQuestions, hardQuestions };
        int diffIdx = "easy".equals(difficulty) ? 0 : "hard".equals(difficulty) ? 2 : 1;

        int qIndex = 0;
        String[] letters = {"A", "B", "C", "D"};

        for (int q = 0; q < choiceCount && qIndex < count; q++) {
            if (q > 0) json.append(",\n");
            String[] opts = {
                "监督学习", "无监督学习", "强化学习", "迁移学习"
            };
            String correct = letters[q % 4];
            // Shuffle options
            String tmp = opts[0]; opts[0] = opts[q % 4]; opts[q % 4] = tmp;

            json.append("    {\n");
            json.append("      \"type\": \"choice\",\n");
            json.append("      \"content\": \"").append(questionsByDiff[diffIdx][q % questionsByDiff[diffIdx].length]).append("\",\n");
            json.append("      \"options\": [\"").append(letters[0]).append(". ").append(opts[0]).append("\", \"")
                .append(letters[1]).append(". ").append(opts[1]).append("\", \"")
                .append(letters[2]).append(". ").append(opts[2]).append("\", \"")
                .append(letters[3]).append(". ").append(opts[3]).append("\"],\n");
            json.append("      \"answer\": \"").append(correct).append("\",\n");
            json.append("      \"explanation\": \"该题考查").append(questionsByDiff[diffIdx][q % questionsByDiff[diffIdx].length]).append("相关概念。").append(correct).append("选项正确。\"\n");
            json.append("    }");
            qIndex++;
        }

        for (int q = 0; q < blankCount && qIndex < count; q++) {
            if (q > 0 || choiceCount > 0) json.append(",\n");
            String[] blanks = {
                "深度学习中的\"深度\"指的是神经网络的______层数。",
                "机器学习通常分为监督学习、______学习和强化学习三大类。",
                "CNN的中文全称是______神经网络。",
                "Transformer架构中使用的核心机制是______注意力。",
                "反向传播算法用于优化神经网络的______。",
                "在机器学习中，训练集用于______模型，测试集用于评估模型性能。"
            };
            String[] answers = {"隐藏", "无监督", "卷积", "自", "权重", "训练"};
            json.append("    {\n");
            json.append("      \"type\": \"blank\",\n");
            json.append("      \"content\": \"").append(blanks[q % blanks.length]).append("\",\n");
            json.append("      \"answer\": \"").append(answers[q % answers.length]).append("\",\n");
            json.append("      \"explanation\": \"填空题考查对基本概念的理解，正确答案为：").append(answers[q % answers.length]).append("。\"\n");
            json.append("    }");
            qIndex++;
        }

        for (int q = 0; q < essayCount && qIndex < count; q++) {
            if (q > 0 || choiceCount > 0 || blankCount > 0) json.append(",\n");
            String[] essays = {
                "请简述人工智能、机器学习和深度学习三者之间的关系。",
                "列举人工智能在教育领域的3个应用场景，并说明其原理。",
                "解释什么是过拟合，如何避免过拟合？",
                "深度学习在计算机视觉中有哪些经典应用？请举例说明。",
                "你认为人工智能技术在未来10年将如何改变教育行业？"
            };
            json.append("    {\n");
            json.append("      \"type\": \"essay\",\n");
            json.append("      \"content\": \"").append(essays[q % essays.length]).append("\",\n");
            json.append("      \"answer\": \"（参考答案）该题要求结合所学知识进行综合分析，答案要点：...。\",\n");
            json.append("      \"explanation\": \"简答题考查对概念的综合理解和应用能力，答题时应注意逻辑清晰、要点完整。\"\n");
            json.append("    }");
            qIndex++;
        }

        json.append("\n  ]\n}");
        return json.toString();
    }

    private String generateMaterialResponse(String prompt) {
        return """
        # AI Teacher Studio - 课件内容（模拟数据）

        ## PPT大纲

        **幻灯片1：封面**
        - 标题：人工智能导论
        - 副标题：探索智能科技的奥秘
        - 学校/姓名：[填写]

        **幻灯片2：目录**
        - 什么是人工智能
        - 人工智能的发展历史
        - 人工智能的应用领域
        - 人工智能的未来展望

        **幻灯片3：什么是人工智能**
        - 定义：使机器具有人类智能的技术
        - 核心目标：感知、推理、学习、决策
        - 关键特征：自主性、适应性、交互性

        **幻灯片4-10：详细内容（略）**

        **幻灯片11：总结**
        - 人工智能改变生活
        - 学习AI知识的重要性
        - 课后思考问题

        **幻灯片12：致谢**
        - 感谢聆听
        - 欢迎提问
        """;
    }

    private String generateKnowledgeResponse(String prompt) {
        return """
        # AI Teacher Studio - 知识点整理（模拟数据）

        ## 核心知识点

        ### 1. 人工智能（AI）定义
        人工智能（Artificial Intelligence）是研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统的一门新的技术科学。

        ### 2. 机器学习分类
        - **监督学习**：有标签数据训练（分类、回归）
        - **无监督学习**：无标签数据（聚类、降维）
        - **强化学习**：通过奖励机制学习（游戏、机器人）

        ### 3. 神经网络基础
        - 神经元：信息处理单元
        - 层：输入层、隐藏层、输出层
        - 权重：神经元之间的连接强度
        - 激活函数：ReLU、Sigmoid、Tanh

        ### 4. 深度学习特点
        - 多层隐藏层
        - 自动特征提取
        - 端到端学习
        - 大数据驱动

        ### 5. 常见应用场景
        - 图像识别：人脸识别、自动驾驶
        - 自然语言处理：机器翻译、聊天机器人
        - 推荐系统：短视频推荐、电商推荐
        """;
    }

    private String generateVideoScriptResponse(String prompt) {
        return """
        # AI Teacher Studio - 视频脚本（模拟数据）

        ## 视频主题：人工智能入门

        **时长**：5-8分钟

        ## 开头（0:00-0:30）
        主持人：大家好，今天我们来聊聊人工智能。你是否好奇，为什么手机能识别你的脸？为什么推荐算法知道你喜欢什么？让我们一起探索AI的奥秘！

        ## 第一部分：什么是AI（0:30-2:00）
        主持人：人工智能，简称AI，简单来说就是让计算机具有"思考"能力...
        [插入动画演示：AI概念图]
        主持人：AI不是魔法，而是数学和代码的结合...

        ## 第二部分：生活中的AI（2:00-4:00）
        主持人：其实AI就在我们身边..."
        [插入实际应用画面：智能音箱、自动驾驶、推荐算法]
        主持人：每天你都在和AI打交道！

        ## 第三部分：如何学习AI（4:00-6:00）
        主持人：想学习AI？从这里开始..."
        [插入学习路径图]
        主持人：Python是入门AI的好选择...

        ## 结尾（6:00-7:00）
        主持人：今天的分享就到这里。希望大家对AI有了新的认识。我们下期再见！

        [片尾字幕 + 关注提示]
        """;
    }

    private String generateDefaultResponse(String prompt) {
        return "【模拟AI回复】\n\n您好！我是AI助教（模拟模式）。\n\n您的请求已收到，正在处理中...\n\n提示：当前使用的是模拟AI Provider，如需使用真实AI能力，请配置有效的API Key。\n\n示例：在.env中设置 AI_MINIMAX_API_KEY=您的真实密钥，然后重启后端服务。";
    }

    private int estimateTokens(String text) {
        // Rough estimate: ~0.75 words per token for Chinese, ~0.25 chars per token
        return Math.max(1, text.length() / 4);
    }
}
