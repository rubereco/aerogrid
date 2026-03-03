package com.aerogrid.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts every HTTP request exactly once and performs
 * JWT-based authentication.
 *
 * <p>The filter follows this processing pipeline for each request:</p>
 * <ol>
 *   <li>Read the {@code Authorization} header and check for a {@code Bearer} token.</li>
 *   <li>Extract the user's email (subject claim) from the JWT.</li>
 *   <li>Load the user from the database via {@link UserDetailsService}.</li>
 *   <li>Validate the token's signature and expiration.</li>
 *   <li>Register a {@link UsernamePasswordAuthenticationToken} in the
 *       {@link SecurityContextHolder} so the rest of the filter chain treats the
 *       request as authenticated.</li>
 * </ol>
 *
 * <p>If no valid token is present the request is passed through unchanged,
 * letting Spring Security's access-control rules decide whether to allow or
 * reject it.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Service used to extract claims from and validate JWT tokens.
     */
    private final JwtService jwtService;

    /**
     * Service used to load user details from the data source by username (email).
     */
    private final UserDetailsService userDetailsService;

    /**
     * Core filter method executed once per request.
     *
     * <p>Reads the {@code Authorization: Bearer <token>} header, validates the JWT,
     * and — if valid — populates the {@link SecurityContextHolder} with the
     * authenticated user's token so that downstream filters and controllers can
     * access the principal.</p>
     *
     * @param request     the incoming HTTP request; never {@code null}
     * @param response    the outgoing HTTP response; never {@code null}
     * @param filterChain the remaining filter chain to execute after this filter;
     *                    never {@code null}
     * @throws ServletException if a servlet-level error occurs while processing
     *                          the request
     * @throws IOException      if an I/O error occurs while reading the request
     *                          or writing the response
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        userEmail = jwtService.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}