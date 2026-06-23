package com.aiteacher.controller;

import com.aiteacher.entity.Subscription;
import com.aiteacher.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SubscriptionController Integration Tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    void getCurrentSubscription_withoutAuth_shouldReturn403() throws Exception {
        // Spring Security 6: .authenticated() denies without 401, returns 403
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getCurrentSubscription_withAuth_shouldReturnOk() throws Exception {
        // Mock returns null (no subscription = free tier)
        when(subscriptionService.getCurrentSubscription(anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isOk());
    }
}
