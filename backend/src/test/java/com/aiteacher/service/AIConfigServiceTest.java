package com.aiteacher.service;

import com.aiteacher.entity.AiConfig;
import com.aiteacher.mapper.AiConfigMapper;
import com.aiteacher.provider.AIProviderRegistry;
import com.aiteacher.provider.llm.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIConfigServiceTest {

    @Mock
    private AiConfigMapper aiConfigMapper;

    @Mock
    private AIProviderRegistry providerRegistry;

    @InjectMocks
    private AIConfigService aiConfigService;

    @Test
    void testGetActiveProvider() {
        // Given
        LLMProvider mockProvider = mock(LLMProvider.class);
        when(providerRegistry.getActiveProvider()).thenReturn(mockProvider);

        // When
        LLMProvider result = providerRegistry.getActiveProvider();

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetAllProviders() {
        // Given
        List<LLMProvider> mockProviders = Arrays.asList(mock(LLMProvider.class), mock(LLMProvider.class));
        when(providerRegistry.getAllProviders()).thenReturn(mockProviders);

        // When
        List<LLMProvider> result = providerRegistry.getAllProviders();

        // Then
        assertEquals(2, result.size());
    }
}