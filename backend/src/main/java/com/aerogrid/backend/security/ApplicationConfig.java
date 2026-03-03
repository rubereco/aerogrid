package com.aerogrid.backend.security;

import com.aerogrid.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Central Spring Security configuration class that exposes the core authentication
 * beans required by the application.
 *
 * <p>The four beans defined here work together as follows:</p>
 * <ol>
 *   <li>{@link #userDetailsService()} — bridges Spring Security with the database
 *       by loading a user by their email address.</li>
 *   <li>{@link #authenticationProvider()} — wires the {@code UserDetailsService}
 *       and the {@code PasswordEncoder} together so Spring Security can verify
 *       credentials.</li>
 *   <li>{@link #authenticationManager(AuthenticationConfiguration)} — exposes the
 *       {@link AuthenticationManager} so the authentication controller can trigger
 *       login programmatically.</li>
 *   <li>{@link #passwordEncoder()} — defines the BCrypt hashing strategy used
 *       when storing and comparing passwords.</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    /**
     * Repository used to look up users from the database by their email address.
     */
    private final UserRepository userRepository;

    /**
     * Creates a {@link UserDetailsService} that loads a user by email from the database.
     *
     * <p>This bean acts as the bridge between Spring Security's authentication
     * mechanism and the application's persistence layer. It is consumed by
     * {@link #authenticationProvider()}.</p>
     *
     * @return a {@link UserDetailsService} lambda that queries {@link UserRepository}
     *         by email
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuari no trobat"));
    }

    /**
     * Creates a {@link AuthenticationProvider} that uses DAO-based authentication.
     *
     * <p>Combines {@link #userDetailsService()} to load the user and
     * {@link #passwordEncoder()} to verify the supplied password against the
     * BCrypt-hashed value stored in the database.</p>
     *
     * @return a fully configured {@link DaoAuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the {@link AuthenticationManager} from Spring Security's
     * auto-configured {@link AuthenticationConfiguration}.
     *
     * <p>The authentication controller injects this bean to trigger the login
     * flow programmatically (i.e. {@code authenticationManager.authenticate(...)}).</p>
     *
     * @param config Spring Boot's auto-configured {@link AuthenticationConfiguration};
     *               never {@code null}
     * @return the application-wide {@link AuthenticationManager}
     * @throws Exception if the {@link AuthenticationManager} cannot be built
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the password-hashing strategy used throughout the application.
     *
     * <p>BCrypt is used to hash passwords before they are persisted to the database
     * and to verify plain-text passwords against their stored hashes during
     * authentication.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}