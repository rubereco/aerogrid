package com.aerogrid.backend.security.auth;

import com.aerogrid.backend.domain.Role;
import com.aerogrid.backend.domain.User;
import com.aerogrid.backend.repository.UserRepository;
import com.aerogrid.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service that handles user registration and authentication logic.
 *
 * <p>This service acts as the bridge between the {@link AuthenticationController}
 * and the underlying security infrastructure. It is responsible for:</p>
 * <ul>
 *   <li>Creating new user accounts and issuing a JWT on successful registration.</li>
 *   <li>Verifying credentials for existing users and issuing a JWT on successful login.</li>
 * </ul>
 *
 * <p>Passwords are <b>never</b> stored in plain text — they are always hashed
 * with BCrypt via {@link PasswordEncoder} before being persisted.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    /**
     * Repository used to persist and look up {@link User} entities in the database.
     */
    private final UserRepository repository;

    /**
     * Encoder used to hash plain-text passwords with BCrypt before storage,
     * and to verify them during authentication.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Service used to generate signed JWTs for authenticated users.
     */
    private final JwtService jwtService;

    /**
     * Spring Security manager that validates user credentials (email + password)
     * against the database. Throws an exception automatically if validation fails.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user account and issues a JWT upon success.
     *
     * <p>The supplied password is BCrypt-hashed before being stored. The new
     * user is assigned the default {@link Role#USER} role. A signed JWT is
     * generated immediately so the client can start making authenticated
     * requests without a separate login step.</p>
     *
     * @param request the registration payload containing {@code username},
     *                {@code email}, and plain-text {@code password}
     * @return an {@link AuthenticationResponse} containing the signed JWT
     */
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        repository.save(user);

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    /**
     * Authenticates an existing user and issues a JWT upon success.
     *
     * <p>Delegates credential verification to Spring Security's
     * {@link AuthenticationManager}, which checks that the email exists and
     * that the supplied password matches the BCrypt hash stored in the database.
     * If either check fails, an {@link org.springframework.security.core.AuthenticationException}
     * is thrown automatically before this method proceeds further.</p>
     *
     * @param request the login payload containing the user's {@code email}
     *                and plain-text {@code password}
     * @return an {@link AuthenticationResponse} containing the signed JWT
     * @throws org.springframework.security.core.AuthenticationException if the
     *         credentials are invalid or the user does not exist
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}