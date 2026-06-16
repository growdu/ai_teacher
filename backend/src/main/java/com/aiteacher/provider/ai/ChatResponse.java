package com.aiteacher.provider.ai;

import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.UsageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Chat response model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {
    
    private String id;
    private String object;
    private long created;
    private String model;
    private ChatChoice[] choices;
    private UsageInfo usage;
    private String finishReason;
    
    /**
     * Get the content of the first choice
     */
    public String getContent() {
        if (choices != null && choices.length > 0 && choices[0].getMessage() != null) {
            return choices[0].getMessage().getContent();
        }
        return null;
    }
    
    /**
     * Check if the response is empty
     */
    public boolean isEmpty() {
        return getContent() == null || getContent().isEmpty();
    }
}