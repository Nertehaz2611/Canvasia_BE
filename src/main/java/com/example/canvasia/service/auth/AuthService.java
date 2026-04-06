package com.example.canvasia.service.auth;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.LoginRequest;
import com.example.canvasia.dto.auth.RegisterRequest;

public interface AuthService {
    public AuthResponse register(RegisterRequest request);
    public AuthResponse login(LoginRequest request);
}
