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
        if (lower.contains("测验") || lower.contains("quiz") || lower.contains("题目") || lower.contains("试题")) {
            return generateQuizResponse(prompt);
        }

        // Course generation — check AFTER quiz to avoid "课程标题" prefix triggering course route
        if (lower.contains("课程") || lower.contains("course")) {
            return generateCourseResponse(prompt);
        }

        // Teaching material / PPT
        if (lower.contains("教材") || lower.contains("课件") || lower.contains("ppt") || lower.contains("幻灯片")) {
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
