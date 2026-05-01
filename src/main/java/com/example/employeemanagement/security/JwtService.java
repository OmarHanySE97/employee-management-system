package com.example.employeemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Handles JWT creation, claim extraction, and signature validation.
 */
@Service
public class JwtService {

    private final Key signingKey;
    private final long jwtExpiration;

    /**
     * Creates the JWT service using the configured secret and expiration window.
     *
     * @param secret the configured JWT secret
     * @param jwtExpiration the token expiration duration in milliseconds
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {
        this.signingKey = Keys.hmacShaKeyFor(resolveKeyBytes(secret));
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * Extracts the username subject from a JWT.
     *
     * @param token the JWT to inspect
     * @return the username stored in the token subject
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a JWT for the supplied authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return the signed JWT
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(
                "roles",
                userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
        return generateToken(claims, userDetails);
    }

    /**
     * Validates that a JWT belongs to the supplied user and has not expired.
     *
     * @param token the JWT to validate
     * @param userDetails the expected user details
     * @return {@code true} when the token is valid for the supplied user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extracts a claim from the JWT using the supplied resolver function.
     *
     * @param token the JWT to inspect
     * @param claimsResolver the claim extraction function
     * @param <T> the extracted claim type
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a signed JWT with the supplied extra claims.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the authenticated user details
     * @return the signed JWT
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtExpiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Checks whether the supplied JWT is expired.
     *
     * @param token the JWT to inspect
     * @return {@code true} when the token expiration is in the past
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Parses and returns all claims from the supplied JWT.
     *
     * @param token the JWT to parse
     * @return the parsed claims payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Ensures the configured secret meets the minimum key length required by the JWT library.
     *
     * @param secret the configured raw secret
     * @return a byte array suitable for HMAC signing
     */
    private byte[] resolveKeyBytes(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        return Arrays.copyOf(keyBytes, 32);
    }
}
