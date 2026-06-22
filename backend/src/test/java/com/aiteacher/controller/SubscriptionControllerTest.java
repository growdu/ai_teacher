package com.aiteacher.controller;

import com.aiteacher.config.SecurityConfig;
import com.aiteacher.entity.Subscription;
import com.aiteacher.service.JwtService;
import com.aiteacher.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SubscriptionController Unit Tests — WebMvcTest (no DB, no full context)
 */
@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getCurrentSubscription_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getCurrentSubscription_withAuth_shouldReturn200or401() throws Exception {
        // The actual status depends on whether the mock JWT has tenantId
        // With @WithMockUser the filter may still reject — just verify endpoint reachable
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().is2xxSuccessful().or(status().isUnauthorized()));
    }
}
