package com.auth.module.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a refresh token for multi-device session management.
 *
 * Refresh tokens enable users to obtain new access tokens without re-authentication.
 * Each device/login session receives a unique refresh token, allowing:
 * - Multiple concurrent sessions across devices
 * - Device-specific logout (revoking individual tokens)
 * - Security auditing (tracking device info and IP addresses)
 *
 * Key fields:
 * - token: UUID-based unique identifier
 * - expiryDate: Token expiration (default 7 days)
 * - deviceInfo: User-Agent string for device identification
 * - ipAddress: IP address for security tracking
 * - revoked: Flag indicating if token has been invalidated (logout)
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
