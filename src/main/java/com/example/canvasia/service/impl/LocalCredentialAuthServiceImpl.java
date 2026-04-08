package com.example.canvasia.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.canvasia.dto.auth.AuthResponse;
import com.example.canvasia.dto.auth.LoginRequest;
import com.example.canvasia.dto.auth.RegisterRequest;
import com.example.canvasia.entity.User;
import com.example.canvasia.enums.AuthProvider;
import com.example.canvasia.repository.UserRepository;
import com.example.canvasia.service.interfaces.LocalCredentialAuthService;
import com.example.canvasia.service.interfaces.TokenPairFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalCredentialAuthServiceImpl implements LocalCredentialAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPairFactory tokenPairFactory;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Confirm password does not match password");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.create(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName()
        );

        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        return tokenPairFactory.issueForUsername(user.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return tokenPairFactory.issueForUsername(user.getUsername());
    }
}