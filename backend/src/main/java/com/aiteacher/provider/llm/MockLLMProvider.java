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

        // Course generation
        if (lower.contains("课程") || lower.contains("course")) {
            return generateCourseResponse(prompt);
        }

        // Quiz/exam generation
        if (lower.contains("测验") || lower.contains("quiz") || lower.contains("题目") || lower.contains("试题")) {
            return generateQuizResponse(prompt);
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
        return """
        # AI Teacher Studio - 测验题目（模拟数据）

        ## 一、选择题（共10题）

        1. 人工智能的英文缩写是？
        A. AR  B. VR  C. AI  D. ML

        2. 下列不属于机器学习算法的是？
        A. 决策树  B. 神经网络  C. 冒泡排序  D. 支持向量机

        3. 深度学习中的"深度"指的是？
        A. 数据量大  B. 网络层数多  C. 计算复杂  D. 应用广泛

        ## 二、判断题（共5题）

        1. 人工智能可以完全取代人类工作。（×）
        2. 机器学习是人工智能的一个子领域。（✓）
        3. 神经网络受人脑神经元结构启发。（✓）

        ## 三、简答题（共3题）

        1. 请简述人工智能、机器学习和深度学习三者之间的关系。
        2. 列举人工智能在教育领域的3个应用场景。
        3. 如何看待人工智能带来的伦理问题？

        ## 参考答案
        选择题：1.C 2.C 3.B
        判断题：1.× 2.✓ 3.✓
        """;
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
