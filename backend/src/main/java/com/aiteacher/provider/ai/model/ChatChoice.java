package com.aiteacher.provider.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat choice model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatChoice {
    private int index;
    private ChatMessage message;
    private String finishReason;
}