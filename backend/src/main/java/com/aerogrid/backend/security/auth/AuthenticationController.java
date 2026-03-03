package com.aerogrid.backend.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the public authentication endpoints for user
 * registration and login.
 *
 * <p>All routes are prefixed with {@code /api/v1/auth} and are explicitly
 * whitelisted in {@link com.aerogrid.backend.security.SecurityConfig} so they
 * can be reached without a JWT token.</p>
 *
 * <p>On success both endpoints return an {@link AuthenticationResponse} containing
 * a signed JWT that the client must include in the {@code Authorization: Bearer}
 * header of every subsequent protected request.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    /**
     * Service that contains the business logic for registration and authentication.
     */
    private final AuthenticationService service;

    /**
     * Registers a new user account and returns a signed JWT.
     *
     * <p><b>POST</b> {@code /api/v1/auth/register}</p>
     *
     * <p>The supplied password is BCrypt-hashed before storage. The new user is
     * assigned the default {@code USER} role. A JWT is issued immediately so the
     * client does not need a separate login step after registering.</p>
     *
     * @param request the registration payload containing {@code username},
     *                {@code email}, and plain-text {@code password}
     * @return {@code 200 OK} with an {@link AuthenticationResponse} containing
     *         the signed JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    /**
     * Authenticates an existing user and returns a signed JWT.
     *
     * <p><b>POST</b> {@code /api/v1/auth/login}</p>
     *
     * <p>Validates the supplied {@code email} and {@code password} against the
     * database. Returns {@code 403 Forbidden} automatically if the credentials
     * are invalid.</p>
     *
     * @param request the login payload containing the user's {@code email}
     *                and plain-text {@code password}
     * @return {@code 200 OK} with an {@link AuthenticationResponse} containing
     *         the signed JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
}