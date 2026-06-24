package com.aiteacher.config;

import com.aiteacher.provider.AIProviderRegistry;
import com.aiteacher.provider.llm.*;
import com.aiteacher.provider.video.MiniMaxVideoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AIProviderConfig {

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${ai.openai.model:gpt-4o}")
    private String openaiModel;

    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.base-url:https://api.anthropic.com}")
    private String claudeBaseUrl;

    @Value("${ai.claude.model:claude-3-5-sonnet-20240620}")
    private String claudeModel;

    @Value("${ai.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;

    @Value("${ai.qwen.model:qwen-max}")
    private String qwenModel;

    @Value("${ai.minimax.api-key:}")
    private String minimaxApiKey;

    @Value("${ai.minimax.base-url:https://api.minimax.chat/v1}")
    private String minimaxBaseUrl;

    @Value("${ai.minimax.model:MiniMax-Text-01}")
    private String minimaxModel;

    @Value("${ai.minimax.video-api-key:}")
    private String minimaxVideoApiKey;

    @Value("${ai.mock.enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private AIProviderRegistry registry;

    @Bean
    public void configureAIProviders() {
        // Force mock mode if enabled - skip all real providers to avoid 401 errors
        if (mockEnabled) {
            log.info("AI Mock mode enabled - using mock provider only");
            MockLLMProvider mockProvider = new MockLLMProvider();
            registry.registerLLMProvider("mock", mockProvider);
            return;
        }

        // Register OpenAI provider if API key is set
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            OpenAILLMProvider openaiProvider = new OpenAILLMProvider(openaiApiKey, openaiBaseUrl, openaiModel);
            registry.registerLLMProvider("openai", openaiProvider);
            log.info("Registered OpenAI LLM provider: {}", openaiModel);
        }

        // Register Claude provider if API key is set
        if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
            ClaudeLLMProvider claudeProvider = new ClaudeLLMProvider(claudeApiKey, claudeBaseUrl, claudeModel);
            registry.registerLLMProvider("claude", claudeProvider);
            log.info("Registered Claude LLM provider: {}", claudeModel);
        }

        // Register Qwen provider if API key is set
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            QwenLLMProvider qwenProvider = new QwenLLMProvider(qwenApiKey, qwenBaseUrl, qwenModel);
            registry.registerLLMProvider("qwen", qwenProvider);
            log.info("Registered Qwen LLM provider: {}", qwenModel);
        }

        // Register MiniMax provider if API key is set
        if (minimaxApiKey != null && !minimaxApiKey.isEmpty()) {
            MiniMaxLLMProvider minimaxProvider = new MiniMaxLLMProvider(minimaxApiKey, minimaxBaseUrl, minimaxModel);
            registry.registerLLMProvider("minimax", minimaxProvider);
            log.info("Registered MiniMax LLM provider: {}", minimaxModel);
        }

        // Register MiniMax Video provider if API key is set
        if (minimaxVideoApiKey != null && !minimaxVideoApiKey.isEmpty()) {
            MiniMaxVideoProvider videoProvider = new MiniMaxVideoProvider(minimaxVideoApiKey);
            log.info("Registered MiniMax Video provider");
        }

        // Register Mock provider as fallback only when no real providers are available
        if (registry.getLLMProviders().isEmpty()) {
            log.warn("No LLM providers registered - using Mock provider");
            MockLLMProvider mockProvider = new MockLLMProvider();
            registry.registerLLMProvider("mock", mockProvider);
        }
    }

    @Bean
    public MiniMaxVideoProvider miniMaxVideoProvider() {
        if (minimaxVideoApiKey == null || minimaxVideoApiKey.isEmpty()) {
            return null;
        }
        return new MiniMaxVideoProvider(minimaxVideoApiKey);
    }
}
