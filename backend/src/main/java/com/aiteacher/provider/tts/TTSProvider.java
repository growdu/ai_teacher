package com.aiteacher.provider.tts;

import com.aiteacher.provider.ai.AIProvider;
import com.aiteacher.provider.ai.VoiceConfig;

import java.io.InputStream;

/**
 * TTS Provider interface
 */
public interface TTSProvider extends AIProvider {
    
    /**
     * Synthesize speech from text
     * @param text The text to synthesize
     * @param voiceConfig Voice configuration
     * @return Audio data as InputStream
     */
    InputStream synthesize(String text, VoiceConfig voiceConfig);
    
    /**
     * Get supported voice list
     */
    VoiceConfig[] getSupportedVoices();
}