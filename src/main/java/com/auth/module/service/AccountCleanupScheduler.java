package com.auth.module.service;

import com.auth.module.model.User;
import com.auth.module.model.VerificationToken;
import com.auth.module.repository.UserRepository;
import com.auth.module.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for cleaning up unverified accounts and expired tokens
 *
 * Runs daily to:
 * - Delete expired verification tokens
 * - Delete unverified accounts older than 24 hours
 * - Send warning emails before deletion (optional)
 */
@Service
public class AccountCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AccountCleanupScheduler.class);

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Clean up expired tokens and unverified accounts
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        logger.info("Starting scheduled cleanup of unverified accounts and expired tokens");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusHours(24);

        try {
            // Find expired tokens
            List<VerificationToken> expiredTokens = tokenRepository.findExpiredTokens(now);
            logger.info("Found {} expired verification tokens", expiredTokens.size());

            int deletedAccounts = 0;

            // Delete associated unverified accounts
            for (VerificationToken token : expiredTokens) {
                User user = token.getUser();

                // Only delete if user is not verified
                if (!user.getEmailVerified()) {
                    logger.info("Deleting unverified account: {} (created: {})",
                               user.getEmail(), user.getCreatedAt());
                    userRepository.delete(user);
                    deletedAccounts++;
                }
            }

            // Delete expired tokens
            int deletedTokens = tokenRepository.deleteExpiredTokens(now);
            logger.info("Deleted {} expired tokens and {} unverified accounts",
                       deletedTokens, deletedAccounts);

            logger.info("Cleanup completed successfully");

        } catch (Exception e) {
            logger.error("Error during cleanup process", e);
        }
    }

    /**
     * Send warning emails to users with expiring tokens
     * Runs every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void sendExpirationWarnings() {
        logger.info("Checking for accounts nearing expiration");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningThreshold = now.plusHours(12); // 12 hours before expiration

            // Find tokens expiring soon
            List<VerificationToken> expiringTokens = tokenRepository.findAll().stream()
                    .filter(token -> token.getExpiresAt().isAfter(now)
                                  && token.getExpiresAt().isBefore(warningThreshold)
                                  && !token.isVerified())
                    .toList();

            logger.info("Found {} accounts nearing expiration", expiringTokens.size());

            for (VerificationToken token : expiringTokens) {
                User user = token.getUser();

                if (!user.getEmailVerified()) {
                    try {
                        long hoursRemaining = java.time.Duration.between(now, token.getExpiresAt()).toHours();
                        emailService.sendAccountDeletionWarning(
                                user.getEmail(),
                                user.getName(),
                                (int) hoursRemaining
                        );
                        logger.info("Sent expiration warning to: {}", user.getEmail());
                    } catch (Exception e) {
                        logger.error("Failed to send warning email to: {}", user.getEmail(), e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error during expiration warning process", e);
        }
    }
}
