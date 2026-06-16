package com.aiteacher.provider.ai.model;

import java.io.Serializable;

/**
 * Chat message model
 */
public class ChatMessage implements Serializable {
    
    private String role;  // "user", "assistant", "system"
    private String content;
    private String name;
    
    public ChatMessage() {}
    
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // Getters and Setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    // Builder pattern
    public static ChatMessageBuilder builder() { return new ChatMessageBuilder(); }
    
    public static class ChatMessageBuilder {
        private String role;
        private String content;
        private String name;
        
        public ChatMessageBuilder role(String role) { this.role = role; return this; }
        public ChatMessageBuilder content(String content) { this.content = content; return this; }
        public ChatMessageBuilder name(String name) { this.name = name; return this; }
        
        public ChatMessage build() {
            ChatMessage msg = new ChatMessage();
            msg.role = this.role;
            msg.content = this.content;
            msg.name = this.name;
            return msg;
        }
    }
    
    /**
     * Create a user message
     */
    public static ChatMessage user(String content) {
        return builder().role("user").content(content).build();
    }
    
    /**
     * Create an assistant message
     */
    public static ChatMessage assistant(String content) {
        return builder().role("assistant").content(content).build();
    }
    
    /**
     * Create a system message
     */
    public static ChatMessage system(String content) {
        return builder().role("system").content(content).build();
    }
}
