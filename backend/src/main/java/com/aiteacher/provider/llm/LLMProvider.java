package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.AIProvider;
import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.EmbeddingRequest;
import com.aiteacher.provider.ai.EmbeddingResponse;
import com.aiteacher.provider.ai.model.ProviderType;

import java.util.List;

/**
 * LLM Provider interface
 */
public interface LLMProvider extends AIProvider {
    
    /**
     * Get the provider type
     */
    ProviderType getProviderType();
    
    /**
     * Send a chat request and get a synchronous response
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Generate embeddings for the given texts
     */
    EmbeddingResponse embedding(EmbeddingRequest request);
}
