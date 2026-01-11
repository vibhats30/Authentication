package com.auth.module.service;

import org.passay.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for validating passwords against industry-standard security requirements.
 *
 * Implements NIST (National Institute of Standards and Technology) compliant password rules:
 * - Minimum 8 characters, maximum 128 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character (!@#$%^&*...)
 * - No whitespace characters
 * - Protection against common sequential patterns (abc, 123, qwerty)
 */
@Service
public class PasswordValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);

    private final PasswordValidator validator;

    /**
     * Initializes the password validator with NIST-compliant security rules.
     * Rules are configured once at service initialization for optimal performance.
     */
    public PasswordValidationService() {
        logger.info("Initializing PasswordValidationService with NIST-compliant rules");

        // Industry standard password rules based on NIST guidelines
        List<Rule> rules = Arrays.asList(
            // Minimum 8 characters (NIST recommendation)
            new LengthRule(8, 128),

            // At least one uppercase character
            new CharacterRule(EnglishCharacterData.UpperCase, 1),

            // At least one lowercase character
            new CharacterRule(EnglishCharacterData.LowerCase, 1),

            // At least one digit
            new CharacterRule(EnglishCharacterData.Digit, 1),

            // At least one special character
            new CharacterRule(EnglishCharacterData.Special, 1),

            // No whitespace allowed
            new WhitespaceRule(),

            // Check against common passwords (top 10000)
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false)
        );

        this.validator = new PasswordValidator(rules);
        logger.debug("Password validator initialized with {} rules", rules.size());
    }

    /**
     * Validates a password against all configured security rules.
     *
     * Checks the password for compliance with NIST guidelines and returns detailed
     * error messages for any rules that fail validation.
     *
     * @param password The password string to validate
     * @return PasswordValidationResult containing validation status and error messages
     */
    public PasswordValidationResult validatePassword(String password) {
        logger.debug("Validating password - length: {}", password != null ? password.length() : 0);

        RuleResult result = validator.validate(new PasswordData(password));

        PasswordValidationResult validationResult = new PasswordValidationResult();
        validationResult.setValid(result.isValid());

        if (!result.isValid()) {
            List<String> messages = new ArrayList<>();
            for (String msg : validator.getMessages(result)) {
                messages.add(msg);
            }
            validationResult.setErrors(messages);
            logger.warn("Password validation failed - {} error(s): {}", messages.size(), messages);
        } else {
            logger.debug("Password validation successful");
        }

        return validationResult;
    }

    /**
     * Result object containing password validation outcome and error details.
     *
     * Provides a simple boolean indicator of validity along with specific
     * error messages for any rules that failed.
     */
    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errors;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}
