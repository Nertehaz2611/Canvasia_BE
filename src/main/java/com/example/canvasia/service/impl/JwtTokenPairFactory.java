package com.example.canvasia.service.impl;

import org.springframework.stereotype.Service;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.security.jwt.JwtService;
import com.example.canvasia.service.interfaces.TokenPairFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenPairFactory implements TokenPairFactory {

    private static final String TOKEN_TYPE = "Bearer";

    private final JwtService jwtService;

    @Override
    public AuthResponse issueForUsername(String username) {
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE);
    }
}