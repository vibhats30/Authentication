package com.auth.module.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordValidationService.
 * Tests NIST-compliant password validation rules.
 */
@DisplayName("PasswordValidationService Tests")
class PasswordValidationServiceTest {

    private PasswordValidationService passwordValidationService;

    @BeforeEach
    void setUp() {
        passwordValidationService = new PasswordValidationService();
    }

    @Test
    @DisplayName("Should validate correct password successfully")
    void shouldValidateCorrectPassword() {
        // Given
        String validPassword = "SecurePass123!";

        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(validPassword);

        // Then
        assertTrue(result.isValid());
        assertNull(result.getErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pass123!",      // Too short (7 chars)
            "short1!",       // Too short
            "1234567"        // Too short
    })
    @DisplayName("Should reject passwords shorter than 8 characters")
    void shouldRejectShortPasswords(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.contains("8") || error.toLowerCase().contains("length")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password123!",   // No uppercase
            "test12345!",     // No uppercase
            "secure@pass1"    // No uppercase
    })
    @DisplayName("Should reject passwords without uppercase letters")
    void shouldRejectPasswordsWithoutUppercase(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("uppercase")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PASSWORD123!",   // No lowercase
            "SECURE@PASS1",   // No lowercase
            "TEST12345!"      // No lowercase
    })
    @DisplayName("Should reject passwords without lowercase letters")
    void shouldRejectPasswordsWithoutLowercase(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("lowercase")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SecurePass!",    // No digit
            "Password@Test",  // No digit
            "NoNumbers!"      // No digit
    })
    @DisplayName("Should reject passwords without digits")
    void shouldRejectPasswordsWithoutDigits(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("digit")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SecurePass123",  // No special char
            "Password1234",   // No special char
            "Test12345678"    // No special char
    })
    @DisplayName("Should reject passwords without special characters")
    void shouldRejectPasswordsWithoutSpecialChars(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("special")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Secure Pass123!",  // Space in middle
            " SecurePass123!",  // Space at start
            "SecurePass123! "   // Space at end
    })
    @DisplayName("Should reject passwords with whitespace")
    void shouldRejectPasswordsWithWhitespace(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(null);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
    }

    @Test
    @DisplayName("Should reject empty password")
    void shouldRejectEmptyPassword() {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword("");

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ValidPass123!",
            "Secure@Pass1",
            "MyP@ssw0rd",
            "C0mpl3x!Pass",
            "Test123!@#"
    })
    @DisplayName("Should accept various valid password formats")
    void shouldAcceptValidPasswordFormats(String password) {
        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(password);

        // Then
        assertTrue(result.isValid(), "Password '" + password + "' should be valid");
        assertNull(result.getErrors());
    }

    @Test
    @DisplayName("Should provide detailed error messages for invalid password")
    void shouldProvideDetailedErrorMessages() {
        // Given - password missing multiple requirements
        String invalidPassword = "short";

        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(invalidPassword);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().size() > 1, "Should have multiple validation errors");
    }

    @Test
    @DisplayName("Should handle very long valid password")
    void shouldHandleVeryLongPassword() {
        // Given - 128 character password (maximum length)
        String longPassword = "A".repeat(60) + "a".repeat(60) + "1234567!" ;

        // When
        PasswordValidationService.PasswordValidationResult result =
                passwordValidationService.validatePassword(longPassword);

        // Then
        assertTrue(result.isValid());
    }
}
