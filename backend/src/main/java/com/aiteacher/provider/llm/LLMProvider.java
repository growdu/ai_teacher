package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.AIProvider;
import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.EmbeddingRequest;
import com.aiteacher.provider.ai.EmbeddingResponse;

import java.util.stream.Stream;

/**
 * LLM Provider interface
 */
public interface LLMProvider extends AIProvider {
    
    /**
     * Send a chat request and get a synchronous response
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * Send a chat request and get a streaming response
     */
    Stream<ChatResponse> stream(ChatRequest request);
    
    /**
     * Generate embeddings for the given texts
     */
    EmbeddingResponse embedding(EmbeddingRequest request);
}