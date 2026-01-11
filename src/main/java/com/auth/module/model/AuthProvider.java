package com.auth.module.model;

/**
 * Enumeration of supported authentication providers.
 *
 * Identifies how a user authenticated with the system:
 * - LOCAL: Email/password authentication managed by the application
 * - GOOGLE: OAuth2 authentication via Google
 * - FACEBOOK: OAuth2 authentication via Facebook
 * - GITHUB: OAuth2 authentication via GitHub
 * - TWITTER: OAuth2 authentication via Twitter/X
 *
 * Used to prevent authentication conflicts (e.g., preventing OAuth user
 * from logging in with local password).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK,
    GITHUB,
    TWITTER
}
