package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.GoogleLoginRequest;
import com.example.canvasia.dto.auth.LoginRequest;
import com.example.canvasia.dto.auth.RefreshTokenRequest;
import com.example.canvasia.dto.auth.RegisterRequest;

public interface AuthService {
    public AuthResponse register(RegisterRequest request);
    public AuthResponse login(LoginRequest request);
    public AuthResponse loginWithGoogle(GoogleLoginRequest request);
    public AuthResponse refreshToken(RefreshTokenRequest request);
}
