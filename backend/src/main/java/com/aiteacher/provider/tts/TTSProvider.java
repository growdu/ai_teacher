package com.aiteacher.provider.tts;

import com.aiteacher.provider.ai.AIProvider;
import com.aiteacher.provider.ai.VoiceConfig;
import com.aiteacher.provider.ai.model.ProviderType;

import java.io.InputStream;

/**
 * TTS Provider interface
 */
public interface TTSProvider extends AIProvider {
    
    /**
     * Get the provider type
     */
    ProviderType getProviderType();
    
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
