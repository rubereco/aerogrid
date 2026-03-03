package com.aerogrid.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JSON Web Token (JWT) operations such as generation,
 * validation, and claim extraction.
 *
 * <p>The secret key and expiration time are configured via {@code application.properties}
 * using the properties {@code application.security.jwt.secret-key} and
 * {@code application.security.jwt.expiration} respectively.</p>
 */
@Service
public class JwtService {

    /**
     * Base64-encoded HMAC-SHA256 secret key used to sign and verify JWT tokens.
     * Must be at least 256 bits long. Injected from {@code application.properties}.
     */
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * Token validity duration in milliseconds. Injected from {@code application.properties}.
     */
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extracts the username (subject claim) from the given JWT token.
     *
     * @param token the JWT token string
     * @return the username stored in the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a JWT token for the given user with no extra claims.
     *
     * @param userDetails the authenticated user's details
     * @return a signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token for the given user, embedding additional claims.
     *
     * @param extraClaims a map of additional claims to include in the token payload
     *                    (e.g. roles, user ID)
     * @param userDetails the authenticated user's details
     * @return a signed JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates whether a JWT token belongs to the given user and has not expired.
     *
     * @param token       the JWT token string to validate
     * @param userDetails the user whose identity is being verified
     * @return {@code true} if the token is valid for the user and not expired;
     *         {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks whether the given JWT token has expired.
     *
     * @param token the JWT token string
     * @return {@code true} if the token's expiration date is before the current time
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token the JWT token string
     * @return the expiration {@link Date} stored in the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token using the provided resolver function.
     *
     * @param <T>            the type of the claim value to return
     * @param token          the JWT token string
     * @param claimsResolver a function that maps {@link Claims} to the desired value
     * @return the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and returns all claims contained in the given JWT token.
     *
     * @param token the JWT token string
     * @return the {@link Claims} payload of the token
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or the
     *                                      signature is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the Base64 secret key and builds the HMAC-SHA signing key.
     *
     * @return the {@link SecretKey} used to sign and verify JWT tokens
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}