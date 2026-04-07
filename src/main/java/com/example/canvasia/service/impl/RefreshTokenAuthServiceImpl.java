package com.example.canvasia.service.impl;

import org.springframework.stereotype.Service;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.RefreshTokenRequest;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.security.jwt.JwtService;
import com.example.canvasia.service.interfaces.RefreshTokenAuthService;
import com.example.canvasia.service.interfaces.TokenPairFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenAuthServiceImpl implements RefreshTokenAuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenPairFactory tokenPairFactory;

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);

        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return tokenPairFactory.issueForUsername(username);
    }
}