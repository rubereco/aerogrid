package com.aerogrid.backend.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the user registration endpoint ({@code POST /api/v1/auth/register}).
 *
 * <p>All three fields are required. The {@code email} must be unique within the
 * system and will be used as the principal identifier for subsequent logins.
 * The {@code password} will be hashed with BCrypt before being persisted.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    /**
     * The display name chosen by the user.
     * Used for personalisation but not for authentication.
     */
    private String username;

    /**
     * The user's email address.
     * Must be unique across the system; used as the login identifier.
     */
    private String email;

    /**
     * The user's plain-text password.
     * Will be encoded with BCrypt before being stored in the database.
     */
    private String password;
}