package com.aiteacher.service;

import com.aiteacher.entity.AiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AIConfigService Unit Tests
 * Only tests methods that do NOT depend on @Autowired fields (providerRegistry, etc.)
 */
@ExtendWith(MockitoExtension.class)
class AIConfigServiceTest {

    @Mock
    private com.aiteacher.mapper.AiConfigMapper aiConfigMapper;

    @InjectMocks
    private AIConfigService aiConfigService;

    @Test
    void save_shouldReturnTrueOnSuccess() {
        // Given
        AiConfig config = new AiConfig();
        config.setProvider("openai");
        config.setModel("gpt-4o");
        config.setEnabled(true);

        when(aiConfigMapper.insert(any(AiConfig.class))).thenReturn(1);

        // When
        boolean result = aiConfigService.save(config);

        // Then
        assertTrue(result);
        verify(aiConfigMapper).insert(any(AiConfig.class));
    }
}
