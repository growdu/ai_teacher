package com.aiteacher.provider;

import com.aiteacher.provider.llm.LLMProvider;
import com.aiteacher.provider.tts.TTSProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Provider Registry - manages all AI providers
 */
@Slf4j
@Component
public class AIProviderRegistry {

    private final ConcurrentHashMap<String, LLMProvider> llmProviders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TTSProvider> ttsProviders = new ConcurrentHashMap<>();

    /**
     * Register an LLM provider
     */
    public void registerLLMProvider(String name, LLMProvider provider) {
        llmProviders.put(name, provider);
        log.info("Registered LLM provider: {} ({})", name, provider.getProviderType());
    }

    /**
     * Register a TTS provider
     */
    public void registerTTSProvider(String name, TTSProvider provider) {
        ttsProviders.put(name, provider);
        log.info("Registered TTS provider: {} ({})", name, provider.getProviderType());
    }

    /**
     * Get all registered LLM providers sorted by priority
     */
    public List<LLMProvider> getLLMProviders() {
        return llmProviders.values().stream()
                .filter(LLMProvider::isEnabled)
                .sorted(Comparator.comparingInt(LLMProvider::getPriority).reversed())
                .toList();
    }

    /**
     * Get all registered TTS providers sorted by priority
     */
    public List<TTSProvider> getTTSProviders() {
        return ttsProviders.values().stream()
                .filter(TTSProvider::isEnabled)
                .sorted(Comparator.comparingInt(TTSProvider::getPriority).reversed())
                .toList();
    }

    /**
     * Get the best available LLM provider
     */
    public LLMProvider getBestLLMProvider() {
        return getLLMProviders().stream().findFirst().orElse(null);
    }

    /**
     * Get the best available TTS provider
     */
    public TTSProvider getBestTTSProvider() {
        return getTTSProviders().stream().findFirst().orElse(null);
    }

    /**
     * Get LLM provider by name
     */
    public LLMProvider getLLMProvider(String name) {
        return llmProviders.get(name);
    }

    /**
     * Find LLM provider by name (case-insensitive key match + enabled check).
     * Tries: exact key → case-insensitive key → display name → type name.
     */
    public LLMProvider findLLMProvider(String name) {
        if (name == null) return null;
        // exact key
        LLMProvider p = llmProviders.get(name);
        if (p != null && p.isEnabled()) return p;
        // case-insensitive key
        String lower = name.toLowerCase();
        for (Map.Entry<String, LLMProvider> e : llmProviders.entrySet()) {
            if (e.getKey().toLowerCase().equals(lower) && e.getValue().isEnabled()) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Get TTS provider by name
     */
    public TTSProvider getTTSProvider(String name) {
        return ttsProviders.get(name);
    }

    /**
     * Check if any LLM provider is available
     */
    public boolean hasLLMProvider() {
        return !llmProviders.isEmpty();
    }

    /**
     * Check if any TTS provider is available
     */
    public boolean hasTTSProvider() {
        return !ttsProviders.isEmpty();
    }

    /**
     * Remove all providers
     */
    public void clearAll() {
        llmProviders.clear();
        ttsProviders.clear();
    }
}