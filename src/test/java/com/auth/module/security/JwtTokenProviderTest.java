package com.auth.module.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and parsing.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_JWT_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLTI1Ni1iaXQtbWluaW11bQ==";
    private static final Long TEST_EXPIRATION_MS = 60000L; // 1 minute
    private static final Long TEST_REFRESH_EXPIRATION_MS = 300000L; // 5 minutes

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", TEST_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationMs", TEST_REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts separated by dots");
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // Given
        Long userId = 1L;

        // When
        String token = jwtTokenProvider.generateRefreshToken(userId);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("Should validate correct token")
    void shouldValidateCorrectToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(userId, email);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        // Given - token signed with different secret
        JwtTokenProvider differentProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(differentProvider, "jwtSecret",
            "ZGlmZmVyZW50LXNlY3JldC1rZXktZm9yLXRlc3RpbmctMjU2LWJpdC1taW5pbXVt");
        ReflectionTestUtils.setField(differentProvider, "jwtExpirationMs", TEST_EXPIRATION_MS);

        String tokenWithDifferentSignature = differentProvider.generateAccessToken(1L, "test@example.com");

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSignature);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given
        Long expectedUserId = 123L;
        String email = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(expectedUserId, email);

        // When
        Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpirationDateFromToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        Date beforeCreation = new Date();
        String token = jwtTokenProvider.generateAccessToken(userId, email);
        Date afterCreation = new Date();

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(afterCreation));
        // Expiration should be approximately (current time + expiration duration)
        long expectedExpirationTime = beforeCreation.getTime() + TEST_EXPIRATION_MS;
        long actualExpirationTime = expirationDate.getTime();
        assertTrue(Math.abs(actualExpirationTime - expectedExpirationTime) < 1000,
            "Expiration time should be within 1 second of expected");
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() throws InterruptedException {
        // Given - token with very short expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(shortLivedProvider, "jwtExpirationMs", 1L); // 1ms

        String token = shortLivedProvider.generateAccessToken(1L, "test@example.com");

        // Wait for token to expire
        Thread.sleep(10);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should throw exception when extracting user ID from invalid token")
    void shouldThrowExceptionForInvalidTokenUserId() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getUserIdFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should throw exception when extracting expiration from invalid token")
    void shouldThrowExceptionForInvalidTokenExpiration() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getExpirationDateFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // When
        String token1 = jwtTokenProvider.generateAccessToken(userId1, email1);
        String token2 = jwtTokenProvider.generateAccessToken(userId2, email2);

        // Then
        assertNotEquals(token1, token2);
        assertEquals(userId1, jwtTokenProvider.getUserIdFromToken(token1));
        assertEquals(userId2, jwtTokenProvider.getUserIdFromToken(token2));
    }

    @Test
    @DisplayName("Should generate different tokens even for same user at different times")
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String token1 = jwtTokenProvider.generateAccessToken(userId, email);
        Thread.sleep(10); // Small delay to ensure different timestamps
        String token2 = jwtTokenProvider.generateAccessToken(userId, email);

        // Then
        assertNotEquals(token1, token2, "Tokens should be different due to different issuedAt times");
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void shouldHandleNullTokenGracefully() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void shouldHandleEmptyTokenGracefully() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertFalse(isValid);
    }
}
