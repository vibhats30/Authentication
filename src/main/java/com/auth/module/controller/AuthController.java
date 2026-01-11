package com.auth.module.controller;

import com.auth.module.payload.AuthResponse;
import com.auth.module.payload.LoginRequest;
import com.auth.module.payload.SignUpRequest;
import com.auth.module.payload.TokenRefreshRequest;
import com.auth.module.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Registers a new user with email and password.
     *
     * Validates password against industry-standard requirements and creates a new user account.
     * On success, generates JWT access token (15 min) and refresh token (7 days).
     *
     * @param signUpRequest Contains name, email, and password for registration
     * @param request HTTP request to extract device info and IP address
     * @return AuthResponse with access token, refresh token, and token type
     * @throws BadRequestException if email already exists or password validation fails
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                         HttpServletRequest request) {
        logger.info("Received signup request for email: {}", signUpRequest.getEmail());
        logger.debug("Signup request details - IP: {}, Device: {}",
            request.getRemoteAddr(), request.getHeader("User-Agent"));

        try {
            // Add device info and IP address
            signUpRequest.setDeviceInfo(request.getHeader("User-Agent"));
            signUpRequest.setIpAddress(request.getRemoteAddr());

            AuthResponse authResponse = authService.registerUser(signUpRequest);
            logger.info("User registration successful for email: {}", signUpRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            logger.error("User registration failed for email: {} - Error: {}",
                signUpRequest.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Authenticates a user with email and password.
     *
     * Validates credentials and tracks failed login attempts (locks account after 5 failures for 24 hours).
     * On success, generates new JWT access token and device-specific refresh token.
     *
     * @param loginRequest Contains email and password
     * @param request HTTP request to extract device info and IP address
     * @return AuthResponse with access token, refresh token, and token type
     * @throws BadCredentialsException if credentials are invalid
     * @throws LockedException if account is locked due to multiple failed attempts
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                             HttpServletRequest request) {
        logger.info("Received login request for email: {}", loginRequest.getEmail());
        logger.debug("Login request details - IP: {}, Device: {}",
            request.getRemoteAddr(), request.getHeader("User-Agent"));

        try {
            // Add device info and IP address
            loginRequest.setDeviceInfo(request.getHeader("User-Agent"));
            loginRequest.setIpAddress(request.getRemoteAddr());

            AuthResponse authResponse = authService.authenticateUser(loginRequest);
            logger.info("User authentication successful for email: {}", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            logger.error("User authentication failed for email: {} - Error: {}",
                loginRequest.getEmail(), e.getMessage());
            logger.debug("Authentication failure details", e);
            throw e;
        }
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * Validates the refresh token and generates a new access token without requiring user credentials.
     * Supports multi-device sessions where each device has its own refresh token.
     *
     * @param request Contains the refresh token
     * @return AuthResponse with new access token and existing refresh token
     * @throws BadRequestException if refresh token is invalid, expired, or revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        logger.info("Received token refresh request");
        logger.debug("Refresh token (first 10 chars): {}...",
            request.getRefreshToken().substring(0, Math.min(10, request.getRefreshToken().length())));

        try {
            AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
            logger.info("Token refresh successful");
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            logger.error("Token refresh failed - Error: {}", e.getMessage());
            logger.debug("Token refresh failure details", e);
            throw e;
        }
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * Marks the refresh token as revoked, preventing it from being used to generate new access tokens.
     * Only revokes the token for the current device; other devices remain logged in.
     *
     * @param request Contains the refresh token to revoke
     * @return Success message confirming logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody TokenRefreshRequest request) {
        logger.info("Received logout request");
        logger.debug("Logout token (first 10 chars): {}...",
            request.getRefreshToken().substring(0, Math.min(10, request.getRefreshToken().length())));

        try {
            authService.logout(request.getRefreshToken());
            logger.info("User logout successful");
            return ResponseEntity.ok().body("Logged out successfully");
        } catch (Exception e) {
            logger.error("Logout failed - Error: {}", e.getMessage());
            logger.debug("Logout failure details", e);
            throw e;
        }
    }
}
