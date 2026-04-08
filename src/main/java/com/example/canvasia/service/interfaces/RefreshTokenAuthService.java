package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.RefreshTokenRequest;

public interface RefreshTokenAuthService {
    AuthResponse refreshToken(RefreshTokenRequest request);
}