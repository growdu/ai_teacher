package com.aiteacher.provider.ai.model;

/**
 * Usage information model
 */
public class UsageInfo {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    
    public UsageInfo() {}
    
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    
    public static UsageInfoBuilder builder() { return new UsageInfoBuilder(); }
    
    public static class UsageInfoBuilder {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        
        public UsageInfoBuilder promptTokens(int promptTokens) { this.promptTokens = promptTokens; return this; }
        public UsageInfoBuilder completionTokens(int completionTokens) { this.completionTokens = completionTokens; return this; }
        public UsageInfoBuilder totalTokens(int totalTokens) { this.totalTokens = totalTokens; return this; }
        
        public UsageInfo build() {
            UsageInfo u = new UsageInfo();
            u.promptTokens = this.promptTokens;
            u.completionTokens = this.completionTokens;
            u.totalTokens = this.totalTokens;
            return u;
        }
    }
}
