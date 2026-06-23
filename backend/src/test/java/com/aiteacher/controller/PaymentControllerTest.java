package com.aiteacher.controller;

import com.aiteacher.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PaymentController Integration Tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getChannels_withoutAuth_shouldReturnOk() throws Exception {
        // /api/payment/channels is a public endpoint (no auth required)
        mockMvc.perform(get("/api/payment/channels"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getChannels_withAuth_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/payment/channels"))
                .andExpect(status().isOk());
    }
}
