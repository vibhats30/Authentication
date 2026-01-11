package com.auth.module.repository;

import com.auth.module.model.VerificationToken;
import com.auth.module.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for verification token operations
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find token by token string
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Find token by user
     */
    Optional<VerificationToken> findByUser(User user);

    /**
     * Delete all tokens for a user
     */
    void deleteByUser(User user);

    /**
     * Find all expired and unverified tokens
     */
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    List<VerificationToken> findExpiredTokens(LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now AND vt.verifiedAt IS NULL")
    int deleteExpiredTokens(LocalDateTime now);

    /**
     * Check if user has valid token
     */
    @Query("SELECT COUNT(vt) > 0 FROM VerificationToken vt WHERE vt.user = :user AND vt.expiresAt > :now AND vt.verifiedAt IS NULL")
    boolean hasValidToken(User user, LocalDateTime now);
}
