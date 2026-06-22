package com.aiteacher.controller;

import com.aiteacher.service.JwtService;
import com.aiteacher.service.PaymentService;
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
 * PaymentController Unit Tests — WebMvcTest (no DB, no full context)
 */
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getChannels_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/payment/channels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getChannels_withAuth_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/payment/channels"))
                .andExpect(status().isOk());
    }
}
