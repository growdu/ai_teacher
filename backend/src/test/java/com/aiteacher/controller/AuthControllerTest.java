package com.aiteacher.controller;

import com.aiteacher.dto.LoginRequest;
import com.aiteacher.dto.RegisterRequest;
import com.aiteacher.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController Integration Tests
 * Tests login, register, and token refresh endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        com.aiteacher.dto.LoginResponse loginResponse = new com.aiteacher.dto.LoginResponse();
        loginResponse.setToken("mock-jwt-token");
        loginResponse.setUsername("testuser");
        loginResponse.setUserId(1L);
        loginResponse.setRole("user");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(com.aiteacher.common.R.ok(loginResponse));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void login_withMissingUsername_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withValidRequest_shouldReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");

        com.aiteacher.dto.LoginResponse loginResponse = new com.aiteacher.dto.LoginResponse();
        loginResponse.setToken("mock-jwt-token-new");
        loginResponse.setUsername("newuser");
        loginResponse.setUserId(2L);
        loginResponse.setRole("user");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(com.aiteacher.common.R.ok(loginResponse));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void refresh_withValidToken_shouldReturnNewAccessToken() throws Exception {
        when(authService.refreshToken("valid-refresh-token"))
                .thenReturn("new-access-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access-token"));
    }

    @Test
    void refresh_withMissingToken_shouldReturnFail() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("refreshToken is required"));
    }
}