package com.aiteacher.provider.tts;

import com.aiteacher.provider.ai.AIProvider;
import com.aiteacher.provider.ai.VoiceConfig;
import com.aiteacher.provider.ai.model.ProviderType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Aliyun TTS Provider implementation
 */
public class AliyunTTSProvider implements TTSProvider {

    private final String apiKey;
    private final String appKey;
    private final String voice;
    private final String baseUrl;
    private boolean enabled;
    private int priority;

    public AliyunTTSProvider(String apiKey, String appKey, String voice) {
        this.apiKey = apiKey;
        this.appKey = appKey;
        this.voice = voice != null ? voice : "aixia";
        this.baseUrl = "https://nls-gateway-cn-shanghai.aliyuncs.com";
        this.enabled = true;
        this.priority = 10;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ALIYUN_TTS;
    }

    @Override
    public String getProviderName() {
        return ProviderType.ALIYUN_TTS.getDisplayName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public InputStream synthesize(String text, VoiceConfig voiceConfig) {
        try {
            // Using Alibaba Cloud NLS TTS API
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-NLS-Token", apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("appkey", appKey);
            requestBody.put("text", text);
            requestBody.put("token_id", apiKey);
            
            Map<String, Object> voice = new HashMap<>();
            voice.put("voice", voiceConfig != null && voiceConfig.getVoiceId() != null 
                    ? voiceConfig.getVoiceId() : this.voice);
            if (voiceConfig != null) {
                if (voiceConfig.getSpeed() != null) voice.put("speech_rate", (voiceConfig.getSpeed() - 1) * 100);
                if (voiceConfig.getPitch() != null) voice.put("pitch_rate", (voiceConfig.getPitch() - 1) * 100);
                if (voiceConfig.getVolume() != null) voice.put("volume", (int)(voiceConfig.getVolume() * 100));
            }
            requestBody.put("voice", voice);
            
            requestBody.put("format", "mp3");
            requestBody.put("sample_rate", 16000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/stream/v1/tts",
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Response is base64 encoded audio
                byte[] audioData = Base64.getDecoder().decode(response.getBody());
                return new ByteArrayInputStream(audioData);
            }
            
            throw new RuntimeException("Aliyun TTS request failed: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Aliyun TTS synthesize failed: " + e.getMessage(), e);
        }
    }

    @Override
    public VoiceConfig[] getSupportedVoices() {
        return new VoiceConfig[]{
                VoiceConfig.builder().voiceId("aixia").language("zh-CN").accent("中文").build(),
                VoiceConfig.builder().voiceId("aiqi").language("zh-CN").accent("中文").build(),
                VoiceConfig.builder().voiceId("aiaddin").language("zh-CN").accent("中文").build(),
                VoiceConfig.builder().voiceId("aiyu").language("zh-CN").accent("中文").build(),
                VoiceConfig.builder().voiceId("aijohn").language("en-US").accent("英文").build(),
                VoiceConfig.builder().voiceId("aijose").language("en-US").accent("英文").build(),
        };
    }
}