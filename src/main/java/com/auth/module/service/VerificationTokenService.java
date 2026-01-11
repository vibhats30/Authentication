package com.auth.module.service;

import com.auth.module.exception.BadRequestException;
import com.auth.module.model.User;
import com.auth.module.model.VerificationToken;
import com.auth.module.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing email verification tokens
 */
@Service
public class VerificationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Value("${app.verification.token-expiration-hours:24}")
    private int tokenExpirationHours;

    /**
     * Create verification token for user
     *
     * @param user User to create token for
     * @return Generated verification token
     */
    @Transactional
    public VerificationToken createToken(User user) {
        logger.info("Creating verification token for user: {}", user.getEmail());

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate unique token
        String tokenValue = UUID.randomUUID().toString();

        // Create token with expiration
        VerificationToken token = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .build();

        token = tokenRepository.save(token);
        logger.info("Created verification token for user: {}, expires at: {}",
                    user.getEmail(), token.getExpiresAt());

        return token;
    }

    /**
     * Verify token and mark as used
     *
     * @param tokenValue Token string to verify
     * @return User associated with the token
     * @throws BadRequestException if token is invalid, expired, or already used
     */
    @Transactional
    public User verifyToken(String tokenValue) {
        logger.info("Verifying token: {}", tokenValue.substring(0, Math.min(10, tokenValue.length())) + "...");

        VerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    logger.warn("Token not found: {}", tokenValue);
                    return new BadRequestException("Invalid verification token");
                });

        // Check if already verified
        if (token.isVerified()) {
            logger.warn("Token already used: {}", tokenValue);
            throw new BadRequestException("This verification link has already been used");
        }

        // Check if expired
        if (token.isExpired()) {
            logger.warn("Token expired: {}, expiry: {}", tokenValue, token.getExpiresAt());
            throw new BadRequestException("This verification link has expired. Please request a new one.");
        }

        // Mark as verified
        token.setVerifiedAt(LocalDateTime.now());
        tokenRepository.save(token);

        logger.info("Token verified successfully for user: {}", token.getUser().getEmail());
        return token.getUser();
    }

    /**
     * Check if user has valid token
     *
     * @param user User to check
     * @return true if user has valid non-expired token
     */
    public boolean hasValidToken(User user) {
        return tokenRepository.hasValidToken(user, LocalDateTime.now());
    }

    /**
     * Get token for user
     *
     * @param user User to get token for
     * @return Token if exists
     */
    public VerificationToken getTokenForUser(User user) {
        return tokenRepository.findByUser(user).orElse(null);
    }
}
