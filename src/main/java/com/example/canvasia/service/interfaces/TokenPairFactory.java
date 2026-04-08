package com.example.canvasia.service.interfaces;

import com.example.canvasia.dto.auth.AuthResponse;

public interface TokenPairFactory {
    AuthResponse issueForUsername(String username);
}