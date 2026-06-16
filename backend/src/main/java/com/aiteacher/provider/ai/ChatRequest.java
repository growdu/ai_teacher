package com.aiteacher.provider.ai;

import com.aiteacher.provider.ai.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Chat request model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest implements Serializable {
    
    private List<ChatMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    private Boolean stream;
    private Map<String, Object> extraParams;
    
    /**
     * Create a simple chat request with user message
     */
    public static ChatRequest of(String userMessage) {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.user(userMessage)))
                .build();
    }
    
    /**
     * Create a chat request with messages and model
     */
    public static ChatRequest of(List<ChatMessage> messages, String model) {
        return ChatRequest.builder()
                .messages(messages)
                .model(model)
                .build();
    }
}