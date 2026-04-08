package com.example.canvasia.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";
    private static final String USERNAME_REGEX = "^[a-z0-9._]+$";

    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 30, message = "Username must be between 1 and 30 characters")
    @Pattern(
            regexp = USERNAME_REGEX,
            message = "Username can only contain lowercase letters, numbers, dot (.) and underscore (_)"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email format")
    private String email;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = PASSWORD_REGEX,
            message = "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
