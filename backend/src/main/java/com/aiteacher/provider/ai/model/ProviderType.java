package com.aiteacher.provider.ai.model;

/**
 * AI Provider type enumeration
 */
public enum ProviderType {
    OPENAI("OpenAI"),
    CLAUDE("Claude"),
    QWEN("Qwen"),
    MINIMAX("MiniMax"),
    DEEPSEEK("DeepSeek"),
    MOCK("Mock LLM"),
    ALIYUN_TTS("Aliyun TTS"),
    VOLCENGINE_TTS("Volcengine TTS");
    
    private final String displayName;
    
    ProviderType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}