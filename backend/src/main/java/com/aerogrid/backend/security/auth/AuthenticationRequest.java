package com.aerogrid.backend.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the user login endpoint ({@code POST /api/v1/auth/authenticate}).
 *
 * <p>The {@code email} and {@code password} are validated against the stored
 * BCrypt-hashed credentials. On success, the server returns an
 * {@link AuthenticationResponse} containing a signed JWT.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    /**
     * The user's registered email address, used as the login identifier.
     */
    private String email;

    /**
     * The user's plain-text password to be verified against the BCrypt hash
     * stored in the database.
     */
    private String password;
}