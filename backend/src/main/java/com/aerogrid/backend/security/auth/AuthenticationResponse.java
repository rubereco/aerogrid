package com.aerogrid.backend.security.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned by both the register and login endpoints.
 *
 * <p>Contains a single signed JWT that the client must store and attach to
 * every subsequent request as an {@code Authorization: Bearer <token>} header.
 * The token encodes the user's identity and expiry information; no server-side
 * session is maintained.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    /**
     * The signed JWT issued after a successful authentication or registration.
     * Must be included in the {@code Authorization: Bearer <token>} header of
     * every protected API request.
     */
    private String token;
}