package com.example.canvasia.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties props;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes());
    }

    public String generateToken(String username) {
        return generateAccessToken(username);
    }

    public String generateAccessToken(String username) {
        return buildToken(username, props.getExpiration(), ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(String username) {
        Long refreshExpiration = props.getRefreshExpiration() != null
                ? props.getRefreshExpiration()
                : props.getExpiration() * 7;
        return buildToken(username, refreshExpiration, REFRESH_TOKEN_TYPE);
    }

    private String buildToken(String username, long expirationMillis, String tokenType) {
        return Jwts.builder()
                .setSubject(username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSignKey())
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        if (!isAccessTokenType(claims)) {
            throw new RuntimeException("Invalid access token");
        }
        return claims.getSubject();
    }

    public String extractUsernameFromRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        if (!isRefreshTokenType(claims)) {
            throw new RuntimeException("Invalid refresh token");
        }
        return claims.getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            return isAccessTokenType(claims)
                    && claims.getSubject().equals(userDetails.getUsername())
                    && !isTokenExpired(claims);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return isRefreshTokenType(claims) && !isTokenExpired(claims);
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    private boolean isAccessTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        // Backward compatibility: tokens without token_type are treated as access tokens.
        return tokenType == null || ACCESS_TOKEN_TYPE.equals(tokenType);
    }

    private boolean isRefreshTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        return REFRESH_TOKEN_TYPE.equals(tokenType);
    }
}