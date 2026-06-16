package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.EmbeddingRequest;
import com.aiteacher.provider.ai.EmbeddingResponse;
import com.aiteacher.provider.ai.model.ProviderType;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Abstract LLM Provider with common functionality
 */
public abstract class AbstractLLMProvider implements LLMProvider {

    protected String apiKey;
    protected String baseUrl;
    protected String model;
    protected boolean enabled;
    protected int priority;

    @Override
    public String getProviderName() {
        return getProviderType().getDisplayName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public EmbeddingResponse embedding(EmbeddingRequest request) {
        throw new UnsupportedOperationException("Embedding not supported by " + getProviderName());
    }

    /**
     * Build headers for API requests
     */
    protected Map<String, String> buildHeaders() {
        return Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + apiKey
        );
    }

    /**
     * Convert ChatRequest to provider-specific format and call chat API
     */
    protected abstract ChatResponse doChat(ChatRequest request);

    /**
     * Convert ChatRequest to provider-specific format and call streaming API
     */
    protected abstract Flux<String> doStream(ChatRequest request);

    @Override
    public ChatResponse chat(ChatRequest request) {
        return doChat(request);
    }

    @Override
    public Flux<ChatResponse> stream(ChatRequest request) {
        return doStream(request).map(chunk -> ChatResponse.builder()
                .id("")
                .object("chat.completion.chunk")
                .created(System.currentTimeMillis() / 1000)
                .model(request.getModel())
                .build());
    }
}