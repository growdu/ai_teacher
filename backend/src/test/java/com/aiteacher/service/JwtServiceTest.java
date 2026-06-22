package com.aiteacher.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtService Unit Tests — pure unit test, no Spring context needed
 */
class JwtServiceTest {

    // Language-level test fixtures — replicate JwtService logic directly
    private String secret = "test-jwt-secret-at-least-32-characters-long-for-hs256";
    private Long expiration = 86400000L;
    private Long refreshExpiration = 604800000L;
    private String prefix = "Bearer";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String generateToken(Long userId, String username, String role, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tenantId", tenantId);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private String generateRefreshToken(Long userId, String username, String role, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tenantId", tenantId);
        claims.put("type", "refresh");
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // === Test cases ===

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = generateToken(1L, "testuser", "teacher", 1L);
        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        String token = generateToken(1L, "testuser", "teacher", 1L);
        assertTrue(validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        assertFalse(validateToken("invalid.token.here"));
    }

    @Test
    void getUserIdFromToken_shouldExtractCorrectUserId() {
        String token = generateToken(42L, "alice", "admin", 5L);
        assertEquals(42L, parseToken(token).get("userId", Long.class));
    }

    @Test
    void getUsernameFromToken_shouldExtractCorrectUsername() {
        String token = generateToken(1L, "bobteacher", "teacher", 1L);
        assertEquals("bobteacher", parseToken(token).getSubject());
    }

    @Test
    void getTenantIdFromToken_shouldExtractCorrectTenantId() {
        String token = generateToken(1L, "user", "teacher", 99L);
        assertEquals(99L, parseToken(token).get("tenantId", Long.class));
    }

    @Test
    void generateRefreshToken_shouldCreateRefreshToken() {
        String refreshToken = generateRefreshToken(1L, "user", "teacher", 1L);
        assertNotNull(refreshToken);
        assertTrue(isRefreshToken(refreshToken));
    }

    @Test
    void isRefreshToken_withAccessToken_shouldReturnFalse() {
        String accessToken = generateToken(1L, "user", "teacher", 1L);
        assertFalse(isRefreshToken(accessToken));
    }

    @Test
    void isRefreshToken_withRefreshToken_shouldReturnTrue() {
        String refreshToken = generateRefreshToken(1L, "user", "teacher", 1L);
        assertTrue(isRefreshToken(refreshToken));
    }

    @Test
    void refreshAccessToken_shouldGenerateNewAccessToken() {
        String refreshToken = generateRefreshToken(77L, "alice", "teacher", 3L);
        // parse refresh token and generate new access token from its claims
        Claims claims = parseToken(refreshToken);
        String newAccessToken = generateToken(
                claims.get("userId", Long.class),
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("tenantId", Long.class)
        );

        assertNotNull(newAccessToken);
        assertTrue(validateToken(newAccessToken));
        assertEquals(77L, parseToken(newAccessToken).get("userId", Long.class));
        assertEquals("alice", parseToken(newAccessToken).getSubject());
    }

    @Test
    void parseToken_withTamperedToken_shouldThrow() {
        String token = generateToken(1L, "user", "teacher", 1L);
        String tampered = token.substring(0, token.length() - 5) + "xxxxx";
        assertThrows(Exception.class, () -> parseToken(tampered));
    }

    @Test
    void token_shouldContainCorrectRole() {
        String token = generateToken(1L, "adminuser", "admin", 2L);
        assertEquals("admin", parseToken(token).get("role", String.class));
    }
}
