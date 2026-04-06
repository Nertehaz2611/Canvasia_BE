package com.example.canvasia.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String displayName;
    private String password;
}
