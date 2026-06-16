package com.aiteacher.provider.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usage information model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageInfo {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}