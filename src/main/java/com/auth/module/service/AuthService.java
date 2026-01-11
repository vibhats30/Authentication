package com.auth.module.service;

import com.auth.module.exception.BadRequestException;
import com.auth.module.model.AuthProvider;
import com.auth.module.model.RefreshToken;
import com.auth.module.model.User;
import com.auth.module.payload.AuthResponse;
import com.auth.module.payload.LoginRequest;
import com.auth.module.payload.SignUpRequest;
import com.auth.module.repository.UserRepository;
import com.auth.module.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class handling core authentication operations.
 *
 * Provides methods for user registration, login, token refresh, and logout.
 * Implements security features including password validation, account lockout,
 * and multi-device session management.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * Registers a new user with email and password authentication.
     *
     * Validates the password against industry-standard requirements (NIST compliant),
     * encrypts the password using BCrypt, and creates a new user account.
     * Generates initial JWT access and refresh tokens for immediate login.
     *
     * @param signUpRequest User registration details (name, email, password, device info)
     * @return AuthResponse containing access token, refresh token, and token type
     * @throws BadRequestException if email already exists or password validation fails
     */
    @Transactional
    public AuthResponse registerUser(SignUpRequest signUpRequest) {
        logger.info("Starting user registration process for email: {}", signUpRequest.getEmail());

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            logger.warn("Registration failed - Email already exists: {}", signUpRequest.getEmail());
            throw new BadRequestException("Email address already in use.");
        }

        // Validate password
        logger.debug("Validating password for user: {}", signUpRequest.getEmail());
        PasswordValidationService.PasswordValidationResult validationResult =
                passwordValidationService.validatePassword(signUpRequest.getPassword());

        if (!validationResult.isValid()) {
            logger.warn("Registration failed - Password validation failed for email: {} - Errors: {}",
                signUpRequest.getEmail(), validationResult.getErrors());
            throw new BadRequestException("Password validation failed: " +
                    String.join(", ", validationResult.getErrors()));
        }

        logger.debug("Creating new user entity for email: {}", signUpRequest.getEmail());
        User user = User.builder()
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        logger.info("User account created successfully with ID: {} for email: {}",
            savedUser.getId(), savedUser.getEmail());

        logger.debug("Generating access and refresh tokens for userId: {}", savedUser.getId());
        String accessToken = tokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                savedUser.getId(),
                signUpRequest.getDeviceInfo(),
                signUpRequest.getIpAddress()
        );

        logger.info("User registration completed successfully for email: {}", signUpRequest.getEmail());
        return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer");
    }

    /**
     * Authenticates a user with email and password credentials.
     *
     * Implements security features including:
     * - Account lockout after 5 failed attempts (24-hour lock duration)
     * - Automatic unlock after lock duration expires
     * - Failed attempt tracking and reset on successful login
     * - Last login timestamp tracking
     * - Device-specific refresh token generation
     *
     * @param loginRequest Login credentials and device information
     * @return AuthResponse containing access token, refresh token, and token type
     * @throws BadCredentialsException if credentials are invalid
     * @throws LockedException if account is locked due to multiple failed attempts
     */
    @Transactional
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("Starting authentication process for email: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Authentication failed - User not found: {}", loginRequest.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        logger.debug("User found with ID: {}, checking account status", user.getId());

        // Check if account is locked
        if (user.getAccountLocked()) {
            logger.warn("Account is locked for user: {}", loginRequest.getEmail());
            if (user.getLockTime() != null) {
                long lockTimeInMillis = user.getLockTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                long currentTimeInMillis = System.currentTimeMillis();

                if (lockTimeInMillis + LOCK_TIME_DURATION > currentTimeInMillis) {
                    logger.warn("Account still within lock period for user: {}", loginRequest.getEmail());
                    throw new LockedException("Your account has been locked due to multiple failed login attempts. Please try again later.");
                } else {
                    // Unlock account
                    logger.info("Lock period expired, unlocking account for user: {}", loginRequest.getEmail());
                    user.setAccountLocked(false);
                    user.setFailedLoginAttempts(0);
                    user.setLockTime(null);
                    userRepository.save(user);
                }
            }
        }

        try {
            logger.debug("Attempting authentication for user: {}", loginRequest.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication successful for user: {}", loginRequest.getEmail());

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                logger.info("Resetting failed login attempts for user: {}", loginRequest.getEmail());
                user.setFailedLoginAttempts(0);
                user.setAccountLocked(false);
                user.setLockTime(null);
            }

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            logger.debug("Updated last login time for user: {}", loginRequest.getEmail());

            String accessToken = tokenProvider.generateAccessToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user.getId(),
                    loginRequest.getDeviceInfo(),
                    loginRequest.getIpAddress()
            );

            logger.info("Authentication completed successfully for user: {}", loginRequest.getEmail());
            return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer");

        } catch (BadCredentialsException ex) {
            // Increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            logger.warn("Authentication failed for user: {} - Failed attempt count: {}",
                loginRequest.getEmail(), attempts);
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                logger.error("Account locked for user: {} after {} failed attempts",
                    loginRequest.getEmail(), attempts);
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                userRepository.save(user);
                throw new LockedException("Your account has been locked due to multiple failed login attempts.");
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * Validates the refresh token (checks expiration and revocation status),
     * generates a new access token, and returns it with the same refresh token.
     * Supports multi-device sessions where each device maintains its own refresh token.
     *
     * @param refreshTokenStr The refresh token string
     * @return AuthResponse with new access token and existing refresh token
     * @throws BadRequestException if refresh token is invalid, expired, or revoked
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        logger.info("Processing token refresh request");
        logger.debug("Refresh token (first 10 chars): {}...",
            refreshTokenStr.substring(0, Math.min(10, refreshTokenStr.length())));

        return refreshTokenService.findByToken(refreshTokenStr)
                .map(token -> {
                    logger.debug("Refresh token found for userId: {}", token.getUser().getId());
                    return refreshTokenService.verifyExpiration(token);
                })
                .map(RefreshToken::getUser)
                .map(user -> {
                    logger.debug("Generating new access token for userId: {}", user.getId());
                    String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
                    logger.info("Token refresh successful for userId: {}", user.getId());
                    return new AuthResponse(accessToken, refreshTokenStr, "Bearer");
                })
                .orElseThrow(() -> {
                    logger.error("Token refresh failed - Invalid refresh token");
                    return new BadRequestException("Invalid refresh token");
                });
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * Marks the refresh token as revoked in the database, preventing it from being
     * used to generate new access tokens. Only affects the current device; other
     * devices with different refresh tokens remain logged in.
     *
     * @param refreshToken The refresh token to revoke
     */
    @Transactional
    public void logout(String refreshToken) {
        logger.info("Processing logout request");
        logger.debug("Logout token (first 10 chars): {}...",
            refreshToken != null ? refreshToken.substring(0, Math.min(10, refreshToken.length())) : "null");

        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
            logger.info("Logout successful - Refresh token revoked");
        } else {
            logger.warn("Logout called with null refresh token");
        }
    }
}
