package com.aiteacher.service;

import com.aiteacher.provider.AIProviderRegistry;
import com.aiteacher.provider.llm.LLMProvider;
import com.aiteacher.provider.tts.TTSProvider;
import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.VoiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Unified AI Service - provides a simple interface for AI operations
 */
@Slf4j
@Service
public class AIService {

    @Autowired
    private AIProviderRegistry providerRegistry;

    /**
     * Send chat request to the best available LLM
     */
    public String chat(String userMessage) {
        LLMProvider provider = providerRegistry.getBestLLMProvider();
        if (provider == null) {
            throw new RuntimeException("No LLM provider available");
        }
        
        try {
            ChatRequest request = ChatRequest.of(userMessage);
            ChatResponse response = provider.chat(request);
            return response.getContent();
        } catch (Exception e) {
            log.error("LLM chat failed: {}", e.getMessage());
            throw new RuntimeException("LLM chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send chat request with custom messages
     */
    public String chat(List<com.aiteacher.provider.ai.model.ChatMessage> messages) {
        LLMProvider provider = providerRegistry.getBestLLMProvider();
        if (provider == null) {
            throw new RuntimeException("No LLM provider available");
        }
        
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .build();
            ChatResponse response = provider.chat(request);
            return response.getContent();
        } catch (Exception e) {
            log.error("LLM chat failed: {}", e.getMessage());
            throw new RuntimeException("LLM chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send chat request with full control
     */
    public ChatResponse chat(ChatRequest request) {
        LLMProvider provider = providerRegistry.getBestLLMProvider();
        if (provider == null) {
            throw new RuntimeException("No LLM provider available");
        }
        
        return provider.chat(request);
    }

    /**
     * Synthesize speech from text
     */
    public InputStream synthesizeSpeech(String text) {
        return synthesizeSpeech(text, VoiceConfig.defaultConfig());
    }

    /**
     * Synthesize speech with custom voice config
     */
    public InputStream synthesizeSpeech(String text, VoiceConfig voiceConfig) {
        TTSProvider provider = providerRegistry.getBestTTSProvider();
        if (provider == null) {
            throw new RuntimeException("No TTS provider available");
        }
        
        try {
            return provider.synthesize(text, voiceConfig);
        } catch (Exception e) {
            log.error("TTS synthesize failed: {}", e.getMessage());
            throw new RuntimeException("TTS synthesize failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if LLM is available
     */
    public boolean isLLMAvailable() {
        return providerRegistry.hasLLMProvider();
    }

    /**
     * Check if TTS is available
     */
    public boolean isTTSAvailable() {
        return providerRegistry.hasTTSProvider();
    }

    /**
     * Get best LLM provider name
     */
    public String getBestLLMProviderName() {
        LLMProvider provider = providerRegistry.getBestLLMProvider();
        return provider != null ? provider.getProviderName() : null;
    }

    /**
     * Get best TTS provider name
     */
    public String getBestTTSProviderName() {
        TTSProvider provider = providerRegistry.getBestTTSProvider();
        return provider != null ? provider.getProviderName() : null;
    }
}