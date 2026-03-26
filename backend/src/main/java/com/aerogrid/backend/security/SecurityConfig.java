package com.aerogrid.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationProvider;
import java.util.List;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Global HTTP security configuration for the application.
 *
 * <p>This class defines the {@link SecurityFilterChain} that governs every
 * incoming HTTP request. The security model is built around stateless JWT
 * authentication, meaning no HTTP session or cookie is ever created or used
 * by the server.</p>
 *
 * <p>The filter chain applies the following rules in order:</p>
 * <ol>
 *   <li><b>CSRF disabled</b> — not required because the API uses Bearer tokens
 *       instead of browser cookies.</li>
 *   <li><b>Public routes</b> — {@code /api/v1/auth/**} (login &amp; register),
 *       {@code /api/v1/ingest/**} (sensor ingestion via API key), and GET methods on
 *       {@code /api/v1/stations/**} (public map data) are accessible without a
 *       token.</li>
 *   <li><b>Protected routes</b> — every other endpoint requires a valid JWT.</li>
 *   <li><b>Stateless sessions</b> — Spring Security is instructed never to
 *       create or consult an {@code HttpSession}.</li>
 *   <li><b>JWT pre-authentication</b> — {@link JwtAuthenticationFilter} runs
 *       before the default {@link UsernamePasswordAuthenticationFilter} so that
 *       token-based authentication is evaluated first.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Custom filter that extracts and validates the JWT from the
     * {@code Authorization: Bearer <token>} header on every request.
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * DAO-backed authentication provider that verifies user credentials
     * against the database using BCrypt password hashing.
     * Configured in {@link ApplicationConfig#authenticationProvider()}.
     */
    private final AuthenticationProvider authenticationProvider;

    @Value("${application.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Builds and returns the application's {@link SecurityFilterChain}.
     *
     * <p>The chain is configured to:</p>
     * <ul>
     *   <li>Disable CSRF protection (stateless API, no cookies).</li>
     *   <li>Allow unauthenticated access to authentication, ingestion, and
     *       public station endpoints.</li>
     *   <li>Require authentication for all other requests.</li>
     *   <li>Use a {@link SessionCreationPolicy#STATELESS} session policy so
     *       that no server-side session state is ever persisted.</li>
     *   <li>Register the custom {@link JwtAuthenticationFilter} before
     *       Spring's built-in {@link UsernamePasswordAuthenticationFilter}.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security;
     *             never {@code null}
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs while building the filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // -- Public whitelist --
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/ingest/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/stations/**").permitAll()

                        // -- Protected: every other route requires a valid JWT --
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(allowedOrigins); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-KEY"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}