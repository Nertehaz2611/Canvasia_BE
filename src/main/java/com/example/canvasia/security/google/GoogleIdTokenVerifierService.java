package com.example.canvasia.security.google;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoogleIdTokenVerifierService {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final NimbusJwtDecoder jwtDecoder;
    private final String googleClientId;

    public GoogleIdTokenVerifierService(@Value("${google.auth.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
    }

    public GoogleUserInfo verifyAndExtract(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new IllegalStateException("Google auth client-id is not configured");
        }

        final Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        validateIssuer(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        validateAudience(jwt.getAudience());

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified");

        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
            throw new IllegalArgumentException("Google account email is not verified");
        }

        String displayName = jwt.getClaimAsString("name");
        if (!StringUtils.hasText(displayName)) {
            displayName = email;
        }

        return new GoogleUserInfo(email, displayName);
    }

    private void validateIssuer(String issuer) {
        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new IllegalArgumentException("Invalid Google token issuer");
        }
    }

    private void validateAudience(List<String> audience) {
        if (audience == null || !audience.contains(googleClientId)) {
            throw new IllegalArgumentException("Google token audience mismatch");
        }
    }

    public record GoogleUserInfo(String email, String displayName) {
    }
}