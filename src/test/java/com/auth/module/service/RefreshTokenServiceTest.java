package com.auth.module.service;

import com.auth.module.exception.TokenRefreshException;
import com.auth.module.model.AuthProvider;
import com.auth.module.model.RefreshToken;
import com.auth.module.model.User;
import com.auth.module.repository.RefreshTokenRepository;
import com.auth.module.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService.
 * Tests refresh token creation, validation, and management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final Long REFRESH_TOKEN_DURATION_MS = 604800000L; // 7 days

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", REFRESH_TOKEN_DURATION_MS);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_DURATION_MS))
                .deviceInfo("Test Device")
                .ipAddress("192.168.1.1")
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Should create refresh token successfully")
    void shouldCreateRefreshTokenSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(
                1L, "Chrome Browser", "192.168.1.1");

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertNotNull(result.getToken());
        assertTrue(result.getToken().length() > 0);
        assertNotNull(result.getExpiryDate());
        assertEquals("Chrome Browser", result.getDeviceInfo());
        assertEquals("192.168.1.1", result.getIpAddress());
        assertFalse(result.getRevoked());

        verify(userRepository).findById(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when creating token for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            refreshTokenService.createRefreshToken(999L, "Device", "IP");
        });

        verify(userRepository).findById(999L);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void shouldFindRefreshTokenByTokenString() {
        // Given
        String tokenString = testRefreshToken.getToken();
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(testRefreshToken));

        // When
        Optional<RefreshToken> result = refreshTokenService.findByToken(tokenString);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
        verify(refreshTokenRepository).findByToken(tokenString);
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void shouldReturnEmptyWhenTokenNotFound() {
        // Given
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // When
        Optional<RefreshToken> result = refreshTokenService.findByToken("non-existent-token");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should verify valid token successfully")
    void shouldVerifyValidTokenSuccessfully() {
        // Given
        RefreshToken validToken = RefreshToken.builder()
                .token("valid-token")
                .expiryDate(Instant.now().plusMillis(100000))
                .revoked(false)
                .build();

        // When
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        // Then
        assertEquals(validToken, result);
    }

    @Test
    @DisplayName("Should throw exception for revoked token")
    void shouldThrowExceptionForRevokedToken() {
        // Given
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revoked-token")
                .expiryDate(Instant.now().plusMillis(100000))
                .revoked(true)
                .build();

        // When & Then
        TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.verifyExpiration(revokedToken);
        });

        assertTrue(exception.getMessage().contains("revoked"));
    }

    @Test
    @DisplayName("Should throw exception and delete expired token")
    void shouldThrowExceptionAndDeleteExpiredToken() {
        // Given
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .expiryDate(Instant.now().minusMillis(1000))
                .revoked(false)
                .build();

        // When & Then
        TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.verifyExpiration(expiredToken);
        });

        assertTrue(exception.getMessage().contains("expired"));
        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeTokenSuccessfully() {
        // Given
        String tokenString = "token-to-revoke";

        // When
        refreshTokenService.revokeToken(tokenString);

        // Then
        verify(refreshTokenRepository).revokeByToken(tokenString);
    }

    @Test
    @DisplayName("Should revoke all user tokens successfully")
    void shouldRevokeAllUserTokensSuccessfully() {
        // Given
        Long userId = 1L;

        // When
        refreshTokenService.revokeAllUserTokens(userId);

        // Then
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    @DisplayName("Should delete all user tokens successfully")
    void shouldDeleteAllUserTokensSuccessfully() {
        // Given
        Long userId = 1L;

        // When
        refreshTokenService.deleteByUserId(userId);

        // Then
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Should get active tokens for user")
    void shouldGetActiveTokensForUser() {
        // Given
        RefreshToken activeToken1 = RefreshToken.builder()
                .token("token1")
                .expiryDate(Instant.now().plusMillis(100000))
                .revoked(false)
                .build();

        RefreshToken activeToken2 = RefreshToken.builder()
                .token("token2")
                .expiryDate(Instant.now().plusMillis(200000))
                .revoked(false)
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .token("revoked")
                .expiryDate(Instant.now().plusMillis(100000))
                .revoked(true)
                .build();

        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired")
                .expiryDate(Instant.now().minusMillis(1000))
                .revoked(false)
                .build();

        List<RefreshToken> allTokens = Arrays.asList(activeToken1, activeToken2, revokedToken, expiredToken);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(allTokens);

        // When
        List<RefreshToken> activeTokens = refreshTokenService.getActiveTokensForUser(1L);

        // Then
        assertEquals(2, activeTokens.size());
        assertTrue(activeTokens.contains(activeToken1));
        assertTrue(activeTokens.contains(activeToken2));
        assertFalse(activeTokens.contains(revokedToken));
        assertFalse(activeTokens.contains(expiredToken));
    }

    @Test
    @DisplayName("Should throw exception when getting active tokens for non-existent user")
    void shouldThrowExceptionWhenGettingActiveTokensForNonExistentUser() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            refreshTokenService.getActiveTokensForUser(999L);
        });
    }

    @Test
    @DisplayName("Should generate unique tokens for same user")
    void shouldGenerateUniqueTokensForSameUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(System.currentTimeMillis()); // Simulate DB auto-increment
            return token;
        });

        // When
        RefreshToken token1 = refreshTokenService.createRefreshToken(1L, "Device1", "IP1");
        RefreshToken token2 = refreshTokenService.createRefreshToken(1L, "Device2", "IP2");

        // Then
        assertNotEquals(token1.getToken(), token2.getToken());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
