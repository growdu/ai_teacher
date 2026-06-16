package com.aiteacher.exception;

/**
 * Exception thrown when AI provider operation fails
 */
public class AIProviderException extends RuntimeException {
    
    private final String providerName;
    
    public AIProviderException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }
    
    public AIProviderException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
    }
    
    public String getProviderName() {
        return providerName;
    }
}