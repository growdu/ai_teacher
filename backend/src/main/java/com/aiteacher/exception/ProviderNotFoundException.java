package com.aiteacher.exception;

/**
 * Exception thrown when provider is not found
 */
public class ProviderNotFoundException extends RuntimeException {
    
    private final String providerName;
    
    public ProviderNotFoundException(String providerName) {
        super("AI provider not found: " + providerName);
        this.providerName = providerName;
    }
    
    public String getProviderName() {
        return providerName;
    }
}