package com.auth.module.service;

import com.auth.module.exception.TokenRefreshException;
import com.auth.module.model.RefreshToken;
import com.auth.module.model.User;
import com.auth.module.repository.RefreshTokenRepository;
import com.auth.module.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens used in multi-device session management.
 *
 * Refresh tokens allow users to obtain new access tokens without re-entering credentials.
 * Each device receives its own refresh token, enabling concurrent sessions across multiple
 * devices while maintaining security and enabling device-specific logout functionality.
 *
 * Key features:
 * - UUID-based token generation for security
 * - Configurable expiration (default: 7 days)
 * - Device tracking (User-Agent, IP address)
 * - Revocation support for logout
 * - Multi-device session support
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${app.auth.jwt-refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Finds a refresh token by its token string.
     *
     * @param token The refresh token string to search for
     * @return Optional containing the RefreshToken if found, empty otherwise
     */
    public Optional<RefreshToken> findByToken(String token) {
        logger.debug("Looking up refresh token (first 10 chars): {}...",
            token.substring(0, Math.min(10, token.length())));
        Optional<RefreshToken> result = refreshTokenRepository.findByToken(token);
        logger.debug("Token lookup result: {}", result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Creates a new refresh token for a user and device.
     *
     * Generates a UUID-based token with configured expiration duration (default 7 days).
     * Tracks device information and IP address for security auditing.
     * Supports multiple active tokens per user for multi-device sessions.
     *
     * @param userId The ID of the user for whom to create the token
     * @param deviceInfo Device/browser information (typically User-Agent header)
     * @param ipAddress IP address of the device
     * @return The created and persisted RefreshToken
     * @throws RuntimeException if user is not found
     */
    public RefreshToken createRefreshToken(Long userId, String deviceInfo, String ipAddress) {
        logger.info("Creating refresh token for userId: {}", userId);
        logger.debug("Device info: {}, IP: {}", deviceInfo, ipAddress);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Failed to create refresh token - User not found: {}", userId);
                    return new RuntimeException("User not found");
                });

        String tokenValue = UUID.randomUUID().toString();
        logger.debug("Generated refresh token UUID (first 10 chars): {}...",
            tokenValue.substring(0, Math.min(10, tokenValue.length())));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token created successfully with ID: {} for userId: {}",
            saved.getId(), userId);
        return saved;
    }

    /**
     * Verifies that a refresh token is valid and not expired or revoked.
     *
     * Checks both revocation status and expiration time. If the token is expired,
     * it is automatically deleted from the database.
     *
     * @param token The RefreshToken to verify
     * @return The same RefreshToken if valid
     * @throws TokenRefreshException if token is revoked or expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        logger.debug("Verifying refresh token expiration for token ID: {}", token.getId());

        if (token.getRevoked()) {
            logger.warn("Refresh token verification failed - Token is revoked (ID: {})", token.getId());
            throw new TokenRefreshException(token.getToken(), "Refresh token was revoked");
        }

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            logger.warn("Refresh token expired (ID: {}), deleting from database", token.getId());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token was expired. Please make a new signin request");
        }

        logger.debug("Refresh token verification successful (ID: {})", token.getId());
        return token;
    }

    /**
     * Revokes a specific refresh token by marking it as revoked.
     *
     * Used for device-specific logout. The token remains in the database
     * but cannot be used to generate new access tokens.
     *
     * @param token The refresh token string to revoke
     */
    @Transactional
    public void revokeToken(String token) {
        logger.info("Revoking refresh token (first 10 chars): {}...",
            token.substring(0, Math.min(10, token.length())));
        refreshTokenRepository.revokeByToken(token);
        logger.debug("Refresh token revoked successfully");
    }

    /**
     * Revokes all refresh tokens for a specific user.
     *
     * Used for logging out from all devices simultaneously.
     * Marks all user tokens as revoked without deleting them.
     *
     * @param userId The ID of the user whose tokens to revoke
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        logger.info("Revoking all refresh tokens for userId: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("All refresh tokens revoked for userId: {}", userId);
    }

    /**
     * Permanently deletes all refresh tokens for a specific user.
     *
     * Used when deleting a user account or performing cleanup operations.
     * Unlike revocation, this completely removes tokens from the database.
     *
     * @param userId The ID of the user whose tokens to delete
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        logger.info("Deleting all refresh tokens for userId: {}", userId);
        refreshTokenRepository.deleteByUserId(userId);
        logger.info("All refresh tokens deleted for userId: {}", userId);
    }

    /**
     * Retrieves all active (non-revoked, non-expired) refresh tokens for a user.
     *
     * Useful for displaying active sessions or device management features.
     * Filters out both revoked and expired tokens.
     *
     * @param userId The ID of the user
     * @return List of active RefreshTokens
     * @throws RuntimeException if user is not found
     */
    public List<RefreshToken> getActiveTokensForUser(Long userId) {
        logger.info("Retrieving active refresh tokens for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Failed to get active tokens - User not found: {}", userId);
                    return new RuntimeException("User not found");
                });

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUser(user).stream()
                .filter(token -> !token.getRevoked())
                .filter(token -> token.getExpiryDate().isAfter(Instant.now()))
                .toList();

        logger.info("Found {} active refresh token(s) for userId: {}", activeTokens.size(), userId);
        return activeTokens;
    }
}
