package com.aiteacher.provider.ai;

/**
 * AI Provider base interface
 */
public interface AIProvider {
    
    /**
     * Get provider name
     */
    String getProviderName();
    
    /**
     * Check if provider is enabled
     */
    boolean isEnabled();
    
    /**
     * Get provider priority (higher = preferred)
     */
    int getPriority();
}