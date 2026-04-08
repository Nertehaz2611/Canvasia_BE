package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.GoogleLoginRequest;

public interface GoogleAccountAuthService {
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
}