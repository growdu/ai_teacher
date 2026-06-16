package com.aiteacher.provider.ai;

import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.UsageInfo;
import java.io.Serializable;

/**
 * Chat response model
 */
public class ChatResponse implements Serializable {
    
    private String id;
    private String object;
    private long created;
    private String model;
    private ChatChoice[] choices;
    private UsageInfo usage;
    private String finishReason;
    
    public ChatResponse() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public ChatChoice[] getChoices() { return choices; }
    public void setChoices(ChatChoice[] choices) { this.choices = choices; }
    
    public UsageInfo getUsage() { return usage; }
    public void setUsage(UsageInfo usage) { this.usage = usage; }
    
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    
    // Builder
    public static ChatResponseBuilder builder() { return new ChatResponseBuilder(); }
    
    public static class ChatResponseBuilder {
        private String id;
        private String object;
        private long created;
        private String model;
        private ChatChoice[] choices;
        private UsageInfo usage;
        private String finishReason;
        
        public ChatResponseBuilder id(String id) { this.id = id; return this; }
        public ChatResponseBuilder object(String object) { this.object = object; return this; }
        public ChatResponseBuilder created(long created) { this.created = created; return this; }
        public ChatResponseBuilder model(String model) { this.model = model; return this; }
        public ChatResponseBuilder choices(ChatChoice[] choices) { this.choices = choices; return this; }
        public ChatResponseBuilder usage(UsageInfo usage) { this.usage = usage; return this; }
        public ChatResponseBuilder finishReason(String finishReason) { this.finishReason = finishReason; return this; }
        
        public ChatResponse build() {
            ChatResponse r = new ChatResponse();
            r.id = this.id;
            r.object = this.object;
            r.created = this.created;
            r.model = this.model;
            r.choices = this.choices;
            r.usage = this.usage;
            r.finishReason = this.finishReason;
            return r;
        }
    }
    
    /**
     * Get the content of the first choice
     */
    public String getContent() {
        if (choices != null && choices.length > 0 && choices[0].getMessage() != null) {
            return choices[0].getMessage().getContent();
        }
        return null;
    }
    
    /**
     * Check if the response is empty
     */
    public boolean isEmpty() {
        return getContent() == null || getContent().isEmpty();
    }
}
