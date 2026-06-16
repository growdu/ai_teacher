package com.aiteacher.exception;

/**
 * Exception thrown when provider is disabled
 */
public class ProviderDisabledException extends RuntimeException {
    
    private final String providerName;
    
    public ProviderDisabledException(String providerName) {
        super("AI provider is disabled: " + providerName);
        this.providerName = providerName;
    }
    
    public String getProviderName() {
        return providerName;
    }
}