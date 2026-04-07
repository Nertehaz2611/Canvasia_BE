package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.LoginRequest;
import com.example.canvasia.dto.auth.RegisterRequest;

public interface LocalCredentialAuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}