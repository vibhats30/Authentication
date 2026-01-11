package com.auth.module.controller;

import com.auth.module.model.User;
import com.auth.module.repository.UserRepository;
import com.auth.module.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for user-related endpoints.
 * Requires authentication for all endpoints.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieves the currently authenticated user's profile information.
     *
     * Returns user details including name, email, profile image, authentication provider,
     * and assigned roles. This endpoint is protected and requires a valid JWT access token.
     *
     * @param userPrincipal The authenticated user principal extracted from JWT token
     * @return UserResponse containing user profile information (excluding password)
     * @throws RuntimeException if user not found (should not happen for authenticated users)
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        logger.info("Received request to get current user profile for userId: {}", userPrincipal.getId());
        logger.debug("User principal email: {}", userPrincipal.getEmail());

        try {
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.debug("Retrieved user profile for email: {}, provider: {}",
                user.getEmail(), user.getProvider());

            // Create a response DTO (don't send password)
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getImageUrl(),
                    user.getProvider().toString(),
                    user.getRoles()
            );

            logger.info("Successfully returned user profile for userId: {}", userPrincipal.getId());
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            logger.error("Failed to retrieve user profile for userId: {} - Error: {}",
                userPrincipal.getId(), e.getMessage(), e);
            throw e;
        }
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            String imageUrl,
            String provider,
            java.util.Set<String> roles
    ) {}
}
