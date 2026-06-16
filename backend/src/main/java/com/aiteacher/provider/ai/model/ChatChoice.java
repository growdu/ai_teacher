package com.aiteacher.provider.ai.model;

/**
 * Chat choice model
 */
public class ChatChoice {
    private int index;
    private ChatMessage message;
    private String finishReason;
    
    public ChatChoice() {}
    
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    
    public ChatMessage getMessage() { return message; }
    public void setMessage(ChatMessage message) { this.message = message; }
    
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    
    public static ChatChoiceBuilder builder() { return new ChatChoiceBuilder(); }
    
    public static class ChatChoiceBuilder {
        private int index;
        private ChatMessage message;
        private String finishReason;
        
        public ChatChoiceBuilder index(int index) { this.index = index; return this; }
        public ChatChoiceBuilder message(ChatMessage message) { this.message = message; return this; }
        public ChatChoiceBuilder finishReason(String finishReason) { this.finishReason = finishReason; return this; }
        
        public ChatChoice build() {
            ChatChoice c = new ChatChoice();
            c.index = this.index;
            c.message = this.message;
            c.finishReason = this.finishReason;
            return c;
        }
    }
}
