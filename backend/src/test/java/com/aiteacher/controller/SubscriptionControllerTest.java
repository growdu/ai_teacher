package com.aiteacher.controller;

import com.aiteacher.entity.Subscription;
import com.aiteacher.service.JwtService;
import com.aiteacher.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SubscriptionController Unit Tests — WebMvcTest (no DB, no full context)
 */
@WebMvcTest(SubscriptionController.class)
@ActiveProfiles("test")
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
    void getCurrentSubscription_withAuth_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/subscription/current"))
                .andExpect(status().isOk());
    }
}
