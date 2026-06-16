package com.aiteacher.provider.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import com.aiteacher.provider.ai.model.UsageInfo;

/**
 * Embedding response model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse implements Serializable {
    
    private String object;
    private List<EmbeddingData> data;
    private String model;
    private UsageInfo usage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingData implements Serializable {
        private int index;
        private String object;
        private List<Float> embedding;
    }
    
    /**
     * Get the first embedding vector
     */
    public List<Float> getFirstEmbedding() {
        if (data != null && !data.isEmpty()) {
            return data.get(0).getEmbedding();
        }
        return null;
    }
}