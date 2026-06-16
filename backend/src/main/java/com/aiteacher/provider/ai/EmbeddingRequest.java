package com.aiteacher.provider.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Embedding request model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest implements Serializable {
    
    private List<String> texts;
    private String model;
    private String inputType;
    
    /**
     * Create embedding request with single text
     */
    public static EmbeddingRequest of(String text) {
        return EmbeddingRequest.builder()
                .texts(List.of(text))
                .build();
    }
    
    /**
     * Create embedding request with multiple texts
     */
    public static EmbeddingRequest of(List<String> texts) {
        return EmbeddingRequest.builder()
                .texts(texts)
                .build();
    }
}