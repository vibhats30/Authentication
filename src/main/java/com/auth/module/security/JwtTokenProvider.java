package com.auth.module.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT (JSON Web Token) provider for generating and validating access and refresh tokens.
 *
 * Handles JWT operations including:
 * - Access token generation (default 15 minutes expiration)
 * - Refresh token generation (default 7 days expiration)
 * - Token validation and parsing
 * - Secure signing using HMAC-SHA algorithm
 *
 * Tokens contain user ID and email claims and are signed with a secret key
 * configured in application properties.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.jwt-expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.auth.jwt-refresh-expiration-ms}")
    private long jwtRefreshExpirationMs;

    /**
     * Decodes the Base64-encoded JWT secret and creates a secure signing key.
     *
     * @return SecretKey for signing and verifying JWTs
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token from Spring Security Authentication object.
     *
     * Convenience method that extracts user details from the authentication
     * principal and delegates to the main token generation method.
     *
     * @param authentication Spring Security Authentication containing UserPrincipal
     * @return JWT access token string
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        logger.debug("Generating access token from authentication for userId: {}", userPrincipal.getId());
        return generateAccessToken(userPrincipal.getId(), userPrincipal.getEmail());
    }

    /**
     * Generates a JWT access token with user ID and email claims.
     *
     * Creates a signed JWT containing:
     * - Subject: User ID
     * - Claim: User email
     * - Issued at: Current timestamp
     * - Expiration: Configured duration (default 15 minutes)
     *
     * @param userId The user's unique identifier
     * @param email The user's email address
     * @return Signed JWT access token string
     */
    public String generateAccessToken(Long userId, String email) {
        logger.debug("Generating access token for userId: {}, email: {}", userId, email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .subject(Long.toString(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        logger.debug("Access token generated successfully for userId: {} (expires in {} ms)", userId, jwtExpirationMs);
        return token;
    }

    /**
     * Generates a JWT refresh token for long-term authentication.
     *
     * Creates a simpler token containing only the user ID with a longer
     * expiration (default 7 days). Refresh tokens are stored in the database
     * and used to generate new access tokens without re-authentication.
     *
     * @param userId The user's unique identifier
     * @return Signed JWT refresh token string
     */
    public String generateRefreshToken(Long userId) {
        logger.debug("Generating refresh token for userId: {}", userId);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        String token = Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        logger.debug("Refresh token generated successfully for userId: {} (expires in {} ms)", userId, jwtRefreshExpirationMs);
        return token;
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * Parses and validates the token signature, then retrieves the user ID
     * from the token's subject claim.
     *
     * @param token The JWT token string
     * @return The user ID extracted from the token
     * @throws JwtException if token is invalid or cannot be parsed
     */
    public Long getUserIdFromToken(String token) {
        logger.debug("Extracting user ID from JWT token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            logger.debug("Successfully extracted userId: {}", userId);
            return userId;
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates a JWT token's signature and expiration.
     *
     * Verifies that:
     * - Token signature is valid (signed with correct secret key)
     * - Token is not expired
     * - Token format is correct
     *
     * Logs specific errors for different validation failures.
     *
     * @param authToken The JWT token string to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        logger.debug("Validating JWT token");
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            logger.debug("JWT token validation successful");
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * Parses the token and retrieves its expiration timestamp.
     * Useful for checking token validity without full validation.
     *
     * @param token The JWT token string
     * @return The expiration date of the token
     * @throws JwtException if token cannot be parsed
     */
    public Date getExpirationDateFromToken(String token) {
        logger.debug("Extracting expiration date from JWT token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            logger.debug("Token expiration date: {}", expiration);
            return expiration;
        } catch (Exception e) {
            logger.error("Failed to extract expiration date from token: {}", e.getMessage());
            throw e;
        }
    }
}
