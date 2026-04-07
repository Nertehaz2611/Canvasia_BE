package com.example.canvasia.service;

import org.springframework.stereotype.Service;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.GoogleLoginRequest;
import com.example.canvasia.dto.auth.LoginRequest;
import com.example.canvasia.dto.auth.RefreshTokenRequest;
import com.example.canvasia.dto.auth.RegisterRequest;
import com.example.canvasia.service.interfaces.AuthService;
import com.example.canvasia.service.interfaces.GoogleAccountAuthService;
import com.example.canvasia.service.interfaces.LocalCredentialAuthService;
import com.example.canvasia.service.interfaces.RefreshTokenAuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceManager implements AuthService {

    private final LocalCredentialAuthService localCredentialAuthService;
    private final GoogleAccountAuthService googleAccountAuthService;
    private final RefreshTokenAuthService refreshTokenAuthService;

    public AuthResponse register(RegisterRequest request) {

        return localCredentialAuthService.register(request);
    }

    public AuthResponse login(LoginRequest request) {

        return localCredentialAuthService.login(request);
    }

    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {

        return googleAccountAuthService.loginWithGoogle(request);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenAuthService.refreshToken(request);
    }
}
