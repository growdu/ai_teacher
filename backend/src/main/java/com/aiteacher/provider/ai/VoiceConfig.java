package com.aiteacher.provider.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Voice configuration for TTS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceConfig implements Serializable {
    
    private String voiceId;
    private String language;
    private String accent;
    private Double speed;
    private Double pitch;
    private Double volume;
    
    /**
     * Default voice configuration
     */
    public static VoiceConfig defaultConfig() {
        return VoiceConfig.builder()
                .speed(1.0)
                .pitch(1.0)
                .volume(1.0)
                .build();
    }
}