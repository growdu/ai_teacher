package com.aiteacher.service;

import com.aiteacher.entity.AiConfig;
import com.aiteacher.mapper.AiConfigMapper;
import com.aiteacher.provider.AIProviderRegistry;
import com.aiteacher.provider.llm.*;
import com.aiteacher.provider.tts.AliyunTTSProvider;
import com.aiteacher.provider.tts.TTSProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * AI Configuration Service - manages AI provider configurations
 */
@Slf4j
@Service
public class AIConfigService {

    @Autowired
    private AiConfigMapper aiConfigMapper;

    @Autowired
    private AIProviderRegistry providerRegistry;

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url:}")
    private String openaiBaseUrl;

    @Value("${ai.openai.model:}")
    private String openaiModel;

    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.base-url:}")
    private String claudeBaseUrl;

    @Value("${ai.claude.model:}")
    private String claudeModel;

    @Value("${ai.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${ai.qwen.base-url:}")
    private String qwenBaseUrl;

    @Value("${ai.qwen.model:}")
    private String qwenModel;

    @PostConstruct
    public void initProviders() {
        // Initialize from config file first (default providers)
        initDefaultProviders();
        
        // Then initialize from database (tenant-specific providers)
        initDatabaseProviders();
    }

    private void initDefaultProviders() {
        // OpenAI
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            OpenAILLMProvider openaiProvider = new OpenAILLMProvider(
                    openaiApiKey,
                    openaiBaseUrl,
                    openaiModel
            );
            providerRegistry.registerLLMProvider("openai", openaiProvider);
        }

        // Claude
        if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
            ClaudeLLMProvider claudeProvider = new ClaudeLLMProvider(
                    claudeApiKey,
                    claudeBaseUrl,
                    claudeModel
            );
            providerRegistry.registerLLMProvider("claude", claudeProvider);
        }

        // Qwen
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            QwenLLMProvider qwenProvider = new QwenLLMProvider(
                    qwenApiKey,
                    qwenBaseUrl,
                    qwenModel
            );
            providerRegistry.registerLLMProvider("qwen", qwenProvider);
        }
    }

    private void initDatabaseProviders() {
        try {
            List<AiConfig> configs = aiConfigMapper.selectList(
                    new LambdaQueryWrapper<AiConfig>()
                            .eq(AiConfig::getEnabled, true)
                            .orderByDesc(AiConfig::getPriority)
            );

            for (AiConfig config : configs) {
                try {
                    if ("OPENAI".equalsIgnoreCase(config.getProvider())) {
                        OpenAILLMProvider provider = new OpenAILLMProvider(
                                config.getApiKeyEncrypted(),
                                config.getBaseUrl(),
                                config.getModel()
                        );
                        providerRegistry.registerLLMProvider("db_" + config.getId(), provider);
                    } else if ("CLAUDE".equalsIgnoreCase(config.getProvider())) {
                        ClaudeLLMProvider provider = new ClaudeLLMProvider(
                                config.getApiKeyEncrypted(),
                                config.getBaseUrl(),
                                config.getModel()
                        );
                        providerRegistry.registerLLMProvider("db_" + config.getId(), provider);
                    } else if ("QWEN".equalsIgnoreCase(config.getProvider())) {
                        QwenLLMProvider provider = new QwenLLMProvider(
                                config.getApiKeyEncrypted(),
                                config.getBaseUrl(),
                                config.getModel()
                        );
                        providerRegistry.registerLLMProvider("db_" + config.getId(), provider);
                    } else if ("ALIYUN_TTS".equalsIgnoreCase(config.getProvider())) {
                        AliyunTTSProvider provider = new AliyunTTSProvider(
                                config.getApiKeyEncrypted(),
                                config.getApiSecret(),
                                config.getModel()
                        );
                        providerRegistry.registerTTSProvider("db_" + config.getId(), provider);
                    }
                } catch (Exception e) {
                    log.error("Failed to initialize provider {}: {}", config.getProvider(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load AI config from database: {}", e.getMessage());
        }
    }

    /**
     * Reload providers from database
     */
    public void reloadProviders() {
        providerRegistry.clearAll();
        initDefaultProviders();
        initDatabaseProviders();
    }

    /**
     * List AI configs
     */
    public List<AiConfig> list(LambdaQueryWrapper<AiConfig> wrapper) {
        return aiConfigMapper.selectList(wrapper);
    }

    /**
     * Page query AI configs
     */
    public Page<AiConfig> page(Page<AiConfig> page, LambdaQueryWrapper<AiConfig> wrapper) {
        return aiConfigMapper.selectPage(page, wrapper);
    }

    /**
     * Save AI config
     */
    public boolean save(AiConfig aiConfig) {
        return aiConfigMapper.insert(aiConfig) > 0;
    }

    /**
     * Update AI config
     */
    public boolean updateById(AiConfig aiConfig) {
        return aiConfigMapper.updateById(aiConfig) > 0;
    }

    /**
     * Get best LLM provider name
     */
    public String getBestLLMProviderName() {
        LLMProvider provider = providerRegistry.getBestLLMProvider();
        return provider != null ? provider.getProviderType().name() : null;
    }


    /**
     * Get best TTS provider name
     */
    public String getBestTTSProviderName() {
        TTSProvider provider = providerRegistry.getBestTTSProvider();
        return provider != null ? provider.getProviderType().name() : null;
    }

    /**
     * Check if LLM is available
     */
    public boolean isLLMAvailable() {
        return providerRegistry.getBestLLMProvider() != null;
    }

    /**
     * Check if TTS is available
     */
    public boolean isTTSAvailable() {
        return providerRegistry.getBestTTSProvider() != null;
    }

}