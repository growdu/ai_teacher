package com.aiteacher.provider.ai;

import com.aiteacher.provider.ai.model.ChatMessage;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Chat request model
 */
public class ChatRequest implements Serializable {
    
    private List<ChatMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    private Boolean stream;
    private Map<String, Object> extraParams;
    
    public ChatRequest() {}
    
    // Getters and Setters
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    
    public Map<String, Object> getExtraParams() { return extraParams; }
    public void setExtraParams(Map<String, Object> extraParams) { this.extraParams = extraParams; }
    
    // Builder
    public static ChatRequestBuilder builder() { return new ChatRequestBuilder(); }
    
    public static class ChatRequestBuilder {
        private List<ChatMessage> messages;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Integer topK;
        private Boolean stream;
        private Map<String, Object> extraParams;
        
        public ChatRequestBuilder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public ChatRequestBuilder model(String model) { this.model = model; return this; }
        public ChatRequestBuilder temperature(Double temperature) { this.temperature = temperature; return this; }
        public ChatRequestBuilder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public ChatRequestBuilder topP(Double topP) { this.topP = topP; return this; }
        public ChatRequestBuilder topK(Integer topK) { this.topK = topK; return this; }
        public ChatRequestBuilder stream(Boolean stream) { this.stream = stream; return this; }
        public ChatRequestBuilder extraParams(Map<String, Object> extraParams) { this.extraParams = extraParams; return this; }
        
        public ChatRequest build() {
            ChatRequest req = new ChatRequest();
            req.messages = this.messages;
            req.model = this.model;
            req.temperature = this.temperature;
            req.maxTokens = this.maxTokens;
            req.topP = this.topP;
            req.topK = this.topK;
            req.stream = this.stream;
            req.extraParams = this.extraParams;
            return req;
        }
    }
    
    /**
     * Create a simple chat request with user message
     */
    public static ChatRequest of(String userMessage) {
        return builder()
                .messages(List.of(ChatMessage.user(userMessage)))
                .build();
    }
}
