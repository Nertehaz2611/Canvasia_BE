package com.example.canvasia.service.impl;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.GoogleLoginRequest;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.AuthProvider;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.security.google.GoogleIdTokenVerifierService;
import com.example.canvasia.security.google.GoogleIdTokenVerifierService.GoogleUserInfo;
import com.example.canvasia.service.interfaces.GoogleAccountAuthService;
import com.example.canvasia.service.interfaces.TokenPairFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleAccountAuthServiceImpl implements GoogleAccountAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPairFactory tokenPairFactory;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;

    @Override
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUserInfo = googleIdTokenVerifierService.verifyAndExtract(request.getIdToken());

        User user = userRepository.findByEmail(googleUserInfo.email())
                .map(this::linkGoogleProviderIfNeeded)
                .orElseGet(() -> createGoogleUser(googleUserInfo));

        return tokenPairFactory.issueForUsername(user.getUsername());
    }

    private User linkGoogleProviderIfNeeded(User existingUser) {
        if (existingUser.getProvider() != AuthProvider.GOOGLE) {
            existingUser.setProvider(AuthProvider.GOOGLE);
            return userRepository.save(existingUser);
        }
        return existingUser;
    }

    private User createGoogleUser(GoogleUserInfo googleUserInfo) {
        String username = buildUniqueUsernameFromEmail(googleUserInfo.email());

        User user = User.create(
                username,
                googleUserInfo.email(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                googleUserInfo.displayName()
        );
        user.setProvider(AuthProvider.GOOGLE);

        return userRepository.save(user);
    }

    private String buildUniqueUsernameFromEmail(String email) {
        String localPart = email.split("@")[0].toLowerCase(Locale.ROOT);
        String baseUsername = localPart.replaceAll("[^a-z0-9._]", "");
        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        if (baseUsername.length() > 30) {
            baseUsername = baseUsername.substring(0, 30);
        }

        String candidate = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(candidate)) {
            String suffixValue = String.valueOf(suffix++);
            int maxBaseLength = 30 - suffixValue.length();
            String shortenedBase = baseUsername.length() > maxBaseLength
                    ? baseUsername.substring(0, maxBaseLength)
                    : baseUsername;
            candidate = shortenedBase + suffixValue;
        }

        return candidate;
    }
}